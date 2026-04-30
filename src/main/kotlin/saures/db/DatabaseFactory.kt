package saures.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import saures.config.Config

object DatabaseFactory {
    fun init(config: Config) {
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
    }
}
