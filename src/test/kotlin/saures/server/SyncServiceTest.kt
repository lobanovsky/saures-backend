package saures.server

import kotlinx.coroutines.runBlocking
import saures.api.SauresApiClient
import saures.api.model.HistoryPoint
import saures.api.model.Meter
import saures.api.model.MeterState
import saures.api.model.MeterType
import saures.api.model.SauresObject
import saures.api.model.Sensor
import kotlin.test.Test
import kotlin.test.assertEquals

class SyncServiceTest {

    @Test
    fun `getDevices returns meter serial number`() = runBlocking {
        val devices = SyncService(MeterSnSauresApiClient()).getDevices()

        assertEquals("SENSOR-001", devices.single().sensorSn)
        assertEquals("METER-123456", devices.single().meterSn)
    }

    @Test
    fun `getLiveReadings returns meter serial number`() = runBlocking {
        val readings = SyncService(MeterSnSauresApiClient()).getLiveReadings()

        assertEquals("SENSOR-001", readings.single().sensorSn)
        assertEquals("METER-123456", readings.single().meterSn)
    }
}

private class MeterSnSauresApiClient : SauresApiClient {
    override suspend fun login(): String = "sid"

    override suspend fun getObjects(sid: String): List<SauresObject> =
        listOf(
            SauresObject(
                id = 1,
                house = "Москва, Тестовая, 1",
                number = "10",
                label = "Квартира"
            )
        )

    override suspend fun getMeters(sid: String, objectId: Int, date: String?): List<Sensor> =
        listOf(
            Sensor(
                serialNumber = "SENSOR-001",
                meters = listOf(
                    Meter(
                        meterId = 30036,
                        meterName = "ХВС",
                        type = MeterType("Холодная вода", 1),
                        state = MeterState("Ошибок нет", 0),
                        vals = listOf(1670.04),
                        unit = "м³",
                        sn = "METER-123456"
                    )
                )
            )
        )

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
