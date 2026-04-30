package saures.server

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import saures.api.SauresApiClient
import saures.db.ReadingsRepository
import saures.db.SyncedReading
import saures.service.AuthenticatedSession
import saures.service.ReadingsCollector
import java.time.Duration
import java.time.LocalDateTime

class SyncService(client: SauresApiClient) {

    private val session   = AuthenticatedSession(client)
    private val collector = ReadingsCollector(client, session)

    suspend fun sync(): List<SyncedReading> {
        val rows = collector.collectCurrent()
        return ReadingsRepository.insertReadings(rows)
    }

    fun getReadings(limit: Int = 100, meterId: Int? = null): List<SyncedReading> =
        ReadingsRepository.findReadings(limit, meterId)

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
                sensorSn      = row.sensorSn
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
                println("[Scheduler] Next sync at $nextMidnight (in ${hours}h ${minutes}m)")
                delay(delayMs)
                runCatching { sync() }
                    .onSuccess { r -> println("[Scheduler] Synced ${r.size} reading(s) at ${LocalDateTime.now()}") }
                    .onFailure { e -> System.err.println("[Scheduler] Sync failed: ${e.message}") }
            }
        }
    }
}
