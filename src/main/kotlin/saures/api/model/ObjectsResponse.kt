package saures.api.model

import kotlinx.serialization.Serializable

@Serializable
data class ObjectsResponse(
    val data: ObjectsData = ObjectsData(),
    val errors: List<SauresApiError> = emptyList(),
    val status: String
)

@Serializable
data class ObjectsData(
    val objects: List<SauresObject> = emptyList()
)

@Serializable
data class SauresObject(
    val id: Int,
    val house: String,
    val number: String,
    val label: String,
    val enable: Boolean = true,
    val tariffs: List<Tariff> = emptyList()
)

@Serializable
data class Tariff(
    val id: Int,
    val name: String,
    val balance: String = ""
)
