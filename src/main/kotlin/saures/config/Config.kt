package saures.config

data class Config(
    val email: String,
    val password: String,
    val baseUrl: String = "https://api.saures.ru/1.0",
    val dbUrl: String = "jdbc:postgresql://localhost:5456/saures",
    val dbUser: String = "saures",
    val dbPassword: String = "saures",
    val serverPort: Int = 8080,
    val allowedOrigins: List<String> = DEFAULT_ALLOWED_ORIGINS
) {
    companion object {
        val DEFAULT_ALLOWED_ORIGINS = listOf(
            "http://localhost:3000",
            "http://localhost:5173",
            "http://localhost:8080",
            "https://saures.housekpr.ru"
        )

        fun fromEnvironment(): Config {
            val email    = System.getenv("SAURES_EMAIL")
                ?: error("SAURES_EMAIL environment variable is not set")
            val password = System.getenv("SAURES_PASSWORD")
                ?: error("SAURES_PASSWORD environment variable is not set")
            return Config(
                email       = email,
                password    = password,
                dbUrl       = System.getenv("DB_URL")      ?: "jdbc:postgresql://localhost:5456/saures",
                dbUser      = System.getenv("DB_USER")     ?: "saures",
                dbPassword  = System.getenv("DB_PASSWORD") ?: "saures",
                serverPort  = System.getenv("SERVER_PORT")?.toIntOrNull() ?: 8080,
                allowedOrigins = parseAllowedOrigins(System.getenv("ALLOWED_ORIGINS"))
            )
        }

        internal fun parseAllowedOrigins(value: String?): List<String> =
            value
                ?.split(",")
                ?.map { it.trim().trimEnd('/') }
                ?.filter { it.isNotBlank() }
                ?.distinct()
                ?.takeIf { it.isNotEmpty() }
                ?: DEFAULT_ALLOWED_ORIGINS
    }
}
