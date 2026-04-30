package saures.api.model

import kotlinx.serialization.Serializable

@Serializable
data class MeterDataResponse(
    val data: MeterHistoryData,
    val errors: List<String> = emptyList(),
    val status: String
)

@Serializable
data class MeterHistoryData(
    val points: List<HistoryPoint>
)

@Serializable
data class HistoryPoint(
    val datetime: String,
    val vals: List<Double>,
    val state: MeterState
)
