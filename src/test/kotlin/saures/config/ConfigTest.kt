package saures.config

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class ConfigTest {

    @Test
    fun `parseAllowedOrigins returns defaults for blank value`() {
        assertEquals(Config.DEFAULT_ALLOWED_ORIGINS, Config.parseAllowedOrigins(""))
    }

    @Test
    fun `parseAllowedOrigins trims blanks slashes and duplicates`() {
        assertEquals(
            listOf("http://localhost:5173", "https://saures.housekpr.ru"),
            Config.parseAllowedOrigins(
                " http://localhost:5173/ , https://saures.housekpr.ru, http://localhost:5173 "
            )
        )
    }

    @Test
    fun `default origins include production frontend`() {
        assertContains(Config.DEFAULT_ALLOWED_ORIGINS, "https://saures.housekpr.ru")
    }
}
