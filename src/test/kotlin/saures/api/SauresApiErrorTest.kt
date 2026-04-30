package saures.api

import kotlinx.serialization.json.Json
import saures.api.model.ApiException
import saures.api.model.MetersResponse
import saures.api.model.SauresApiError
import saures.api.model.WrongSidException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SauresApiErrorTest {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Test
    fun `parses object errors returned by saures api`() {
        val response = json.decodeFromString<MetersResponse>(
            """
            {
              "status": "bad",
              "errors": [{"name": "WrongSIDException", "msg": "Неверный sid"}],
              "data": {}
            }
            """.trimIndent()
        )

        assertEquals("bad", response.status)
        assertEquals("WrongSIDException", response.errors.single().name)
        assertEquals("Неверный sid", response.errors.single().msg)
    }

    @Test
    fun `parses meter serial number from saures meters response`() {
        val response = json.decodeFromString<MetersResponse>(
            """
            {
              "status": "ok",
              "errors": [],
              "data": {
                "sensors": [{
                  "sn": "CONTROLLER-001",
                  "meters": [{
                    "meter_id": 30036,
                    "meter_name": "ХВС",
                    "sn": "METER-123456",
                    "type": {"name": "Холодная вода", "number": 1},
                    "state": {"name": "Ошибок нет", "number": 0},
                    "vals": [1670.04],
                    "unit": "м³"
                  }]
                }]
              }
            }
            """.trimIndent()
        )

        assertEquals("CONTROLLER-001", response.data.sensors.single().serialNumber)
        assertEquals("METER-123456", response.data.sensors.single().meters.single().sn)
    }

    @Test
    fun `wrong sid api error becomes WrongSidException`() {
        assertFailsWith<WrongSidException> {
            checkApiStatus("bad", listOf(SauresApiError("WrongSIDException", "Неверный sid")))
        }
    }

    @Test
    fun `regular api error becomes ApiException`() {
        val exception = assertFailsWith<ApiException> {
            checkApiStatus("bad", listOf(SauresApiError("SomeException", "Failure")))
        }

        assertEquals("bad", exception.status)
        assertEquals("SomeException", exception.errors.single().name)
    }
}
