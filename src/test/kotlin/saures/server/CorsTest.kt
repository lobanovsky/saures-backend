package saures.server

import io.ktor.client.request.header
import io.ktor.client.request.options
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import saures.api.SauresApiClient
import saures.api.model.HistoryPoint
import saures.api.model.SauresObject
import saures.api.model.Sensor
import saures.config.Config
import kotlin.test.Test
import kotlin.test.assertEquals

class CorsTest {

    @Test
    fun `cors preflight for devices is allowed from configured origin`() = testApplication {
        application {
            configureServer(
                Config(
                    email = "user@example.com",
                    password = "password",
                    allowedOrigins = listOf("http://localhost:5173")
                ),
                SyncService(FakeSauresApiClient())
            )
        }

        val response = client.options("/devices") {
            header(HttpHeaders.Origin, "http://localhost:5173")
            header(HttpHeaders.AccessControlRequestMethod, HttpMethod.Get.value)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("http://localhost:5173", response.headers[HttpHeaders.AccessControlAllowOrigin])
    }
}

private class FakeSauresApiClient : SauresApiClient {
    override suspend fun login(): String = "sid"

    override suspend fun getObjects(sid: String): List<SauresObject> = emptyList()

    override suspend fun getMeters(sid: String, objectId: Int, date: String?): List<Sensor> = emptyList()

    override suspend fun getMeterHistory(
        sid: String,
        meterId: Int,
        start: String,
        finish: String,
        group: String,
        absolute: Boolean
    ): List<HistoryPoint> = emptyList()

    override fun close() = Unit
}
