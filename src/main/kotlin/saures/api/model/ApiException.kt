package saures.api.model

import kotlinx.serialization.Serializable

@Serializable
data class SauresApiError(
    val name: String = "",
    val msg: String = ""
) {
    override fun toString(): String =
        listOf(name, msg).filter { it.isNotBlank() }.joinToString(": ").ifBlank { "Unknown API error" }
}

class WrongSidException(message: String = "SID expired or invalid") : Exception(message)
class ApiException(
    val errors: List<SauresApiError>,
    val status: String
) : Exception("API error ($status): ${errors.joinToString("; ")}")
