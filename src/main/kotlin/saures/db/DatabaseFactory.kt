package saures.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import saures.config.Config

object DatabaseFactory {
    private val logger = LoggerFactory.getLogger(DatabaseFactory::class.java)

    fun init(config: Config) {
        logger.info("Initializing database connection to {}", config.dbUrl)
        val hikari = HikariConfig().apply {
            jdbcUrl         = config.dbUrl
            username        = config.dbUser
            password        = config.dbPassword
            driverClassName = "org.postgresql.Driver"
            maximumPoolSize = 5
            isAutoCommit    = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }
        Database.connect(HikariDataSource(hikari))
        transaction {
            SchemaUtils.createMissingTablesAndColumns(ReadingsTable)
        }
        logger.info("Database schema is ready")
    }
}
