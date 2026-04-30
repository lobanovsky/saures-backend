package saures.server

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import saures.db.SyncedReading
import java.time.LocalDateTime

@Serializable
data class SyncResponse(val synced: Int, val readings: List<SyncedReading>)

@Serializable
data class HealthResponse(val status: String, val timestamp: String)

@Serializable
data class ErrorResponse(val error: String)

fun startServer(port: Int, syncService: SyncService) {
    syncService.startDailySchedule()

    println("Server started on http://0.0.0.0:$port")
    println("  POST /sync                    — trigger manual sync")
    println("  GET  /readings                — stored readings (?limit=100&meter_id=...)")
    println("  GET  /devices                 — list all meters (live)")
    println("  GET  /readings/current        — current readings, all meters (live)")
    println("  GET  /readings/current/{id}   — current readings for one meter (live)")
    println("  GET  /health                  — health check")

    embeddedServer(CIO, port = port, host = "0.0.0.0") {
        install(ContentNegotiation) { json() }

        routing {
            get("/health") {
                call.respond(HealthResponse("ok", LocalDateTime.now().toString()))
            }

            post("/sync") {
                runCatching { syncService.sync() }
                    .onSuccess { readings ->
                        call.respond(SyncResponse(readings.size, readings))
                    }
                    .onFailure { e ->
                        call.respond(HttpStatusCode.InternalServerError, ErrorResponse(e.message ?: "sync failed"))
                    }
            }

            get("/readings") {
                val limit   = call.request.queryParameters["limit"]?.toIntOrNull() ?: 100
                val meterId = call.request.queryParameters["meter_id"]?.toIntOrNull()
                call.respond(syncService.getReadings(limit, meterId))
            }

            get("/devices") {
                runCatching { syncService.getDevices() }
                    .onSuccess { call.respond(it) }
                    .onFailure { e ->
                        call.respond(HttpStatusCode.InternalServerError, ErrorResponse(e.message ?: "failed to fetch devices"))
                    }
            }

            get("/readings/current") {
                runCatching { syncService.getLiveReadings() }
                    .onSuccess { call.respond(it) }
                    .onFailure { e ->
                        call.respond(HttpStatusCode.InternalServerError, ErrorResponse(e.message ?: "failed to fetch current readings"))
                    }
            }

            get("/readings/current/{meterId}") {
                val meterId = call.parameters["meterId"]?.toIntOrNull()
                if (meterId == null) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("meterId must be an integer"))
                    return@get
                }
                runCatching { syncService.getLiveReadings(meterId) }
                    .onSuccess { call.respond(it) }
                    .onFailure { e ->
                        call.respond(HttpStatusCode.InternalServerError, ErrorResponse(e.message ?: "failed to fetch current readings"))
                    }
            }
        }
    }.start(wait = true)
}
