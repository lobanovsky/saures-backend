package saures.service

import kotlinx.coroutines.runBlocking
import saures.api.SauresApiClient
import saures.api.model.HistoryPoint
import saures.api.model.SauresObject
import saures.api.model.Sensor
import saures.api.model.WrongSidException
import kotlin.test.Test
import kotlin.test.assertEquals

class AuthenticatedSessionTest {

    @Test
    fun `refreshes sid and retries block after wrong sid`() = runBlocking {
        val client = FakeSauresApiClient()
        val session = AuthenticatedSession(client)
        val seenSids = mutableListOf<String>()

        val result = session.withSid { sid ->
            seenSids += sid
            if (sid == "expired") throw WrongSidException()
            "ok"
        }

        assertEquals("ok", result)
        assertEquals(listOf("expired", "fresh"), seenSids)
        assertEquals(2, client.loginCalls)
    }
}

private class FakeSauresApiClient : SauresApiClient {
    var loginCalls = 0

    override suspend fun login(): String {
        loginCalls += 1
        return if (loginCalls == 1) "expired" else "fresh"
    }

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
