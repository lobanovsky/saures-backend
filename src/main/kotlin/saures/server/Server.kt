package saures.server

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import saures.db.SyncedReading
import java.time.LocalDateTime

@Serializable
data class SyncResponse(val synced: Int, val readings: List<SyncedReading>)

@Serializable
data class HealthResponse(val status: String, val timestamp: String)

@Serializable
data class ErrorResponse(val error: String)

fun startServer(port: Int, syncService: SyncService) {
    val logger = LoggerFactory.getLogger("Server")
    syncService.startDailySchedule()

    logger.info("Server started on http://0.0.0.0:{}", port)
    logger.info("Routes: POST /sync, GET /readings, GET /devices, GET /readings/current, GET /health")

    embeddedServer(CIO, port = port, host = "0.0.0.0") {
        install(ContentNegotiation) { json() }
        install(CallLogging) {
            level = Level.INFO
        }
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                logger.error("Unhandled error while processing {}", call.request.uri, cause)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ErrorResponse(cause.message ?: "internal server error")
                )
            }
        }

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
                        logger.error("Failed to process {}", call.request.uri, e)
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
                        logger.error("Failed to process {}", call.request.uri, e)
                        call.respond(HttpStatusCode.InternalServerError, ErrorResponse(e.message ?: "failed to fetch devices"))
                    }
            }

            get("/readings/current") {
                runCatching { syncService.getLiveReadings() }
                    .onSuccess { call.respond(it) }
                    .onFailure { e ->
                        logger.error("Failed to process {}", call.request.uri, e)
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
                        logger.error("Failed to process {}", call.request.uri, e)
                        call.respond(HttpStatusCode.InternalServerError, ErrorResponse(e.message ?: "failed to fetch current readings"))
                    }
            }
        }
    }.start(wait = true)
}
