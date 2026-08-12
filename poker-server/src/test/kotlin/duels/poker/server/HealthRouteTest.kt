package duels.poker.server

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class HealthRouteTest {
    @Test
    fun healthAnswersOk() = testApplication {
        application { module() }
        val response = client.get("/health")
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun healthBodyIsOk() = testApplication {
        application { module() }
        val response = client.get("/health")
        assertEquals("OK", response.bodyAsText())
    }

    @Test
    fun anUnknownPathIsNotFound() = testApplication {
        application { module() }
        val response = client.get("/nope")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
