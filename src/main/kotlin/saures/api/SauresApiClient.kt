package saures.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import saures.api.model.*
import saures.config.Config

interface SauresApiClient {
    suspend fun login(): String
    suspend fun getObjects(sid: String): List<SauresObject>
    suspend fun getMeters(sid: String, objectId: Int, date: String? = null): List<Sensor>
    suspend fun getMeterHistory(
        sid: String, meterId: Int,
        start: String, finish: String,
        group: String, absolute: Boolean = true
    ): List<HistoryPoint>
    fun close()
}

class SauresApiClientImpl(private val config: Config) : SauresApiClient {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val http = HttpClient(CIO) {
        install(ContentNegotiation) { json(json) }
        install(Logging) { level = LogLevel.NONE }
        install(HttpRequestRetry) {
            retryOnServerErrors(maxRetries = 2)
            exponentialDelay()
        }
    }

    private fun url(path: String) = "${config.baseUrl}$path"

    override suspend fun login(): String {
        val body: LoginResponse = http.post(url("/login")) {
            setBody(FormDataContent(Parameters.build {
                append("email",    config.email)
                append("password", config.password)
            }))
        }.body()
        body.checkStatus()
        return body.data.sid
    }

    override suspend fun getObjects(sid: String): List<SauresObject> {
        val body: ObjectsResponse = http.get(url("/user/objects")) {
            parameter("sid", sid)
        }.body()
        body.checkStatus()
        return body.data.objects
    }

    override suspend fun getMeters(sid: String, objectId: Int, date: String?): List<Sensor> {
        val body: MetersResponse = http.get(url("/object/meters")) {
            parameter("sid", sid)
            parameter("id",  objectId)
            date?.let { parameter("date", it) }
        }.body()
        body.checkStatus()
        return body.data.sensors
    }

    override suspend fun getMeterHistory(
        sid: String, meterId: Int,
        start: String, finish: String,
        group: String, absolute: Boolean
    ): List<HistoryPoint> {
        val body: MeterDataResponse = http.get(url("/meter/get")) {
            parameter("sid",      sid)
            parameter("id",       meterId)
            parameter("start",    start)
            parameter("finish",   finish)
            parameter("group",    group)
            parameter("absolute", absolute)
        }.body()
        body.checkStatus()
        return body.data.points
    }

    override fun close() = http.close()
}

private fun checkApiStatus(status: String, errors: List<String>) {
    if (status == "bad") {
        if (errors.any { it.contains("WrongSIDException", ignoreCase = true) }) {
            throw WrongSidException()
        }
        throw ApiException(errors, status)
    }
}

private fun LoginResponse.checkStatus()     = checkApiStatus(status, errors)
private fun ObjectsResponse.checkStatus()   = checkApiStatus(status, errors)
private fun MetersResponse.checkStatus()    = checkApiStatus(status, errors)
private fun MeterDataResponse.checkStatus() = checkApiStatus(status, errors)
