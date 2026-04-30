package saures

import saures.api.SauresApiClientImpl
import saures.config.Config
import saures.db.DatabaseFactory
import saures.server.SyncService
import saures.server.startServer
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("Main")

fun main() {
    val config = try {
        Config.fromEnvironment()
    } catch (e: IllegalStateException) {
        logger.error("Configuration error: {}", e.message)
        return
    }
    DatabaseFactory.init(config)
    logger.info("Database connected. Table 'readings' ready.")

    val client      = SauresApiClientImpl(config)
    val syncService = SyncService(client)
    startServer(config, syncService)
}
