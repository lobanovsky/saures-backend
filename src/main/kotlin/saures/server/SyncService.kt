package saures.server

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import saures.api.SauresApiClient
import saures.db.ReadingsRepository
import saures.db.SyncedReading
import saures.service.AuthenticatedSession
import saures.service.ReadingsCollector
import java.time.Duration
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.milliseconds

class SyncService(client: SauresApiClient) {

    private val logger = LoggerFactory.getLogger(SyncService::class.java)
    private val session   = AuthenticatedSession(client)
    private val collector = ReadingsCollector(client, session)

    suspend fun sync(): List<SyncedReading> {
        logger.info("Starting readings sync")
        val rows = collector.collectCurrent()
        val readings = ReadingsRepository.insertReadings(rows)
        logger.info("Readings sync completed: {} reading(s)", readings.size)
        return readings
    }

    fun getReadings(limit: Int = 100, meterId: Int? = null): List<SyncedReading> =
        ReadingsRepository.findReadings(limit, meterId)

    fun getLatestReadings(): List<SyncedReading> =
        ReadingsRepository.findLatestReadingsByMeter()

    suspend fun getDevices(): List<DeviceResponse> =
        collector.collectCurrent().map { row ->
            DeviceResponse(
                meterId       = row.meterId,
                meterName     = row.meterName,
                meterType     = row.meterType,
                unit          = row.unit,
                state         = row.state,
                objectLabel   = row.objectLabel,
                objectAddress = row.objectAddress,
                sensorSn      = row.sensorSn,
                meterSn       = row.meterSn
            )
        }

    suspend fun getLiveReadings(meterId: Int? = null): List<CurrentReadingResponse> {
        val rows = collector.collectCurrent()
        val filtered = if (meterId != null) rows.filter { it.meterId == meterId } else rows
        return filtered.map { row ->
            CurrentReadingResponse(
                meterId       = row.meterId,
                meterName     = row.meterName,
                meterType     = row.meterType,
                unit          = row.unit,
                state         = row.state,
                objectLabel   = row.objectLabel,
                objectAddress = row.objectAddress,
                sensorSn      = row.sensorSn,
                meterSn       = row.meterSn,
                valuePrimary  = row.values.getOrElse(0) { 0.0 },
                valueExtra    = row.values.drop(1).joinToString(";") { "%.3f".format(it) }
            )
        }
    }

    fun startDailySchedule() {
        CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                val now          = LocalDateTime.now()
                val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay()
                val delayMs      = Duration.between(now, nextMidnight).toMillis()
                val hours        = delayMs / 3_600_000
                val minutes      = (delayMs % 3_600_000) / 60_000
                logger.info("Next scheduled sync at {} (in {}h {}m)", nextMidnight, hours, minutes)
                delay(delayMs.milliseconds)
                runCatching { sync() }
                    .onSuccess { r -> logger.info("Scheduled sync completed: {} reading(s)", r.size) }
                    .onFailure { e -> logger.error("Scheduled sync failed", e) }
            }
        }
    }
}
