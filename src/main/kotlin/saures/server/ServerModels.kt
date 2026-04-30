package saures.server

import kotlinx.serialization.Serializable

@Serializable
data class DeviceResponse(
    val meterId: Int,
    val meterName: String,
    val meterType: String,
    val unit: String,
    val state: String,
    val objectLabel: String,
    val objectAddress: String,
    val sensorSn: String,
    val meterSn: String
)

@Serializable
data class CurrentReadingResponse(
    val meterId: Int,
    val meterName: String,
    val meterType: String,
    val unit: String,
    val state: String,
    val objectLabel: String,
    val objectAddress: String,
    val sensorSn: String,
    val meterSn: String,
    val valuePrimary: Double,
    val valueExtra: String
)
