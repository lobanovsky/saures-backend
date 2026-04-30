package saures.service

import saures.api.SauresApiClient
import saures.api.model.WrongSidException
import org.slf4j.LoggerFactory

class AuthenticatedSession(private val client: SauresApiClient) {

    private val logger = LoggerFactory.getLogger(AuthenticatedSession::class.java)
    private var sid: String = ""

    suspend fun <T> withSid(block: suspend (String) -> T): T {
        if (sid.isEmpty()) {
            logger.info("No cached SAURES SID, requesting a new session")
            sid = client.login()
        }
        return try {
            block(sid)
        } catch (_: WrongSidException) {
            logger.info("SAURES SID is invalid or expired, refreshing session")
            sid = client.login()
            block(sid)
        }
    }
}
