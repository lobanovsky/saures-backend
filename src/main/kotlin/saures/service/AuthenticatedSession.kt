package saures.service

import saures.api.SauresApiClient
import saures.api.model.WrongSidException

class AuthenticatedSession(private val client: SauresApiClient) {

    private var sid: String = ""

    suspend fun <T> withSid(block: suspend (String) -> T): T {
        if (sid.isEmpty()) sid = client.login()
        return try {
            block(sid)
        } catch (e: WrongSidException) {
            sid = client.login()
            block(sid)
        }
    }
}
