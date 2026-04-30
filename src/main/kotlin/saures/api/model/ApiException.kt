package saures.api.model

class WrongSidException(message: String = "SID expired or invalid") : Exception(message)
class ApiException(val errors: List<String>, val status: String) : Exception("API error: $errors")
