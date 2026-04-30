package saures.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MetersResponse(
    val data: MetersData = MetersData(),
    val errors: List<SauresApiError> = emptyList(),
    val status: String
)

@Serializable
data class MetersData(
    val sensors: List<Sensor> = emptyList()
)

@Serializable
data class Sensor(
    @SerialName("sn") val serialNumber: String = "",
    val meters: List<Meter> = emptyList()
)

@Serializable
data class Meter(
    @SerialName("meter_id")   val meterId: Int,
    @SerialName("meter_name") val meterName: String,
    val type: MeterType,
    val state: MeterState,
    val vals: List<Double>,
    val unit: String,
    val sn: String = ""
)

@Serializable
data class MeterType(
    val name: String,
    val number: Int
)

@Serializable
data class MeterState(
    val name: String,
    val number: Int
)
