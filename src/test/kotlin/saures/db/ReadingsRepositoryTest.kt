package saures.db

import kotlin.test.Test
import kotlin.test.assertEquals

class ReadingsRepositoryTest {

    @Test
    fun `latestByMeter keeps first reading per meter and sorts for ui`() {
        val readings = listOf(
            reading(id = 1, meterId = 10, objectLabel = "B", meterName = "ХВС", valuePrimary = 10.0),
            reading(id = 2, meterId = 20, objectLabel = "A", meterName = "ГВС", valuePrimary = 20.0),
            reading(id = 3, meterId = 10, objectLabel = "B", meterName = "ХВС", valuePrimary = 9.0)
        )

        val latest = ReadingsRepository.latestByMeter(readings)

        assertEquals(listOf(20, 10), latest.map { it.meterId })
        assertEquals(10.0, latest.single { it.meterId == 10 }.valuePrimary)
    }

    private fun reading(
        id: Long,
        meterId: Int,
        objectLabel: String,
        meterName: String,
        valuePrimary: Double
    ) = SyncedReading(
        id = id,
        syncedAt = "2026-04-30T23:00:00",
        objectLabel = objectLabel,
        objectAddress = "address",
        sensorSn = "sensor",
        meterSn = "meter",
        meterId = meterId,
        meterName = meterName,
        meterType = "type",
        unit = "м³",
        state = "ok",
        valuePrimary = valuePrimary,
        valueExtra = ""
    )
}
