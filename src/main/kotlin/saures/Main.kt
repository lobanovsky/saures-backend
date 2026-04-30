package saures

import saures.api.SauresApiClientImpl
import saures.config.Config
import saures.db.DatabaseFactory
import saures.server.SyncService
import saures.server.startServer

fun main() {
    val config = try {
        Config.fromEnvironment()
    } catch (e: IllegalStateException) {
        System.err.println("Configuration error: ${e.message}")
        return
    }
    DatabaseFactory.init(config)
    println("Database connected. Table 'readings' ready.")

    val client      = SauresApiClientImpl(config)
    val syncService = SyncService(client)
    startServer(config.serverPort, syncService)
}
