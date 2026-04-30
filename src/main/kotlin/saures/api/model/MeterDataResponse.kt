package saures.api.model

import kotlinx.serialization.Serializable

@Serializable
data class MeterDataResponse(
    val data: MeterHistoryData = MeterHistoryData(),
    val errors: List<SauresApiError> = emptyList(),
    val status: String
)

@Serializable
data class MeterHistoryData(
    val points: List<HistoryPoint> = emptyList()
)

@Serializable
data class HistoryPoint(
    val datetime: String,
    val vals: List<Double>,
    val state: MeterState
)
