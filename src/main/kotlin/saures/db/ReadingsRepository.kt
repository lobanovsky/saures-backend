package saures.db

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import saures.service.ReadingRow
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Serializable
data class SyncedReading(
    val id: Long,
    val syncedAt: String,
    val objectLabel: String,
    val objectAddress: String,
    val sensorSn: String,
    val meterSn: String,
    val meterId: Int,
    val meterName: String,
    val meterType: String,
    val unit: String,
    val state: String,
    val valuePrimary: Double,
    val valueExtra: String
)

object ReadingsRepository {

    private val fmt = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    private const val LATEST_READINGS_SCAN_LIMIT = 5_000

    fun insertReadings(rows: List<ReadingRow>): List<SyncedReading> = transaction {
        val now = LocalDateTime.now()
        rows.map { row ->
            val primary = row.values.getOrElse(0) { 0.0 }
            val extra   = row.values.drop(1).joinToString(";") { v -> "%.3f".format(v) }
            val stmt = ReadingsTable.insert {
                it[syncedAt]      = now
                it[objectLabel]   = row.objectLabel
                it[objectAddress] = row.objectAddress
                it[sensorSn]      = row.sensorSn
                it[meterSn]       = row.meterSn
                it[meterId]       = row.meterId
                it[meterName]     = row.meterName
                it[meterType]     = row.meterType
                it[unit]          = row.unit
                it[state]         = row.state
                it[valuePrimary]  = primary
                it[valueExtra]    = extra
            }
            SyncedReading(
                id            = stmt[ReadingsTable.id],
                syncedAt      = now.format(fmt),
                objectLabel   = row.objectLabel,
                objectAddress = row.objectAddress,
                sensorSn      = row.sensorSn,
                meterSn       = row.meterSn,
                meterId       = row.meterId,
                meterName     = row.meterName,
                meterType     = row.meterType,
                unit          = row.unit,
                state         = row.state,
                valuePrimary  = primary,
                valueExtra    = extra
            )
        }
    }

    fun findReadings(limit: Int = 100, meterId: Int? = null): List<SyncedReading> = transaction {
        val query = if (meterId != null) {
            ReadingsTable.selectAll().where { ReadingsTable.meterId eq meterId }
        } else {
            ReadingsTable.selectAll()
        }
        query.orderBy(ReadingsTable.syncedAt to SortOrder.DESC)
            .limit(limit)
            .map { row ->
                SyncedReading(
                    id            = row[ReadingsTable.id],
                    syncedAt      = row[ReadingsTable.syncedAt].format(fmt),
                    objectLabel   = row[ReadingsTable.objectLabel],
                    objectAddress = row[ReadingsTable.objectAddress],
                    sensorSn      = row[ReadingsTable.sensorSn],
                    meterSn       = row[ReadingsTable.meterSn],
                    meterId       = row[ReadingsTable.meterId],
                    meterName     = row[ReadingsTable.meterName],
                    meterType     = row[ReadingsTable.meterType],
                    unit          = row[ReadingsTable.unit],
                    state         = row[ReadingsTable.state],
                    valuePrimary  = row[ReadingsTable.valuePrimary],
                    valueExtra    = row[ReadingsTable.valueExtra]
                )
            }
    }

    fun findLatestReadingsByMeter(): List<SyncedReading> =
        latestByMeter(findReadings(LATEST_READINGS_SCAN_LIMIT))

    internal fun latestByMeter(readings: List<SyncedReading>): List<SyncedReading> =
        readings
            .distinctBy { it.meterId }
            .sortedWith(compareBy<SyncedReading> { it.objectLabel }.thenBy { it.meterName })
}
