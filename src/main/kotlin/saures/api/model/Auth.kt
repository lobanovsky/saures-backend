package saures.api.model

import kotlinx.serialization.Serializable

@Serializable
data class LoginResponse(
    val data: LoginData,
    val errors: List<String> = emptyList(),
    val status: String
)

@Serializable
data class LoginData(
    val sid: String
)
