package saures.service

import saures.api.SauresApiClient

data class ReadingRow(
    val objectLabel: String,
    val objectAddress: String,
    val sensorSn: String,
    val meterId: Int,
    val meterName: String,
    val meterType: String,
    val unit: String,
    val state: String,
    val values: List<Double>,
    val dateTime: String = ""
)

class ReadingsCollector(
    private val client: SauresApiClient,
    private val session: AuthenticatedSession = AuthenticatedSession(client)
) {
    suspend fun collectCurrent(): List<ReadingRow> {
        val objects = session.withSid { client.getObjects(it) }
        return objects.flatMap { obj ->
            val sensors = session.withSid { client.getMeters(it, obj.id) }
            sensors.flatMap { sensor ->
                sensor.meters.map { meter ->
                    ReadingRow(
                        objectLabel   = obj.label,
                        objectAddress = "${obj.house}, кв. ${obj.number}",
                        sensorSn      = sensor.serialNumber,
                        meterId       = meter.meterId,
                        meterName     = meter.meterName,
                        meterType     = meter.type.name,
                        unit          = meter.unit,
                        state         = meter.state.name,
                        values        = meter.vals
                    )
                }
            }
        }
    }

    suspend fun collectHistory(start: String, finish: String, group: String): List<ReadingRow> {
        val objects = session.withSid { client.getObjects(it) }
        val rows = mutableListOf<ReadingRow>()
        for (obj in objects) {
            val sensors = session.withSid { client.getMeters(it, obj.id) }
            for (sensor in sensors) {
                for (meter in sensor.meters) {
                    val points = session.withSid {
                        client.getMeterHistory(it, meter.meterId, start, finish, group)
                    }
                    points.forEach { point ->
                        rows += ReadingRow(
                            objectLabel   = obj.label,
                            objectAddress = "${obj.house}, кв. ${obj.number}",
                            sensorSn      = sensor.serialNumber,
                            meterId       = meter.meterId,
                            meterName     = meter.meterName,
                            meterType     = meter.type.name,
                            unit          = meter.unit,
                            state         = point.state.name,
                            values        = point.vals,
                            dateTime      = point.datetime
                        )
                    }
                }
            }
        }
        return rows
    }
}
