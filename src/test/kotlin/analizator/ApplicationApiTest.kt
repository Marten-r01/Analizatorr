package analizator

import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class ApplicationApiTest {
    @Test
    fun healthEndpointReturnsUp() = testApplication {
        application {
            module()
        }

        val response = client.get("/health")

        assertEquals(HttpStatusCode.OK, response.status)
        assertContains(response.bodyAsText(), "\"status\": \"UP\"")
    }

    @Test
    fun analyzeEndpointReturnsProteins() = testApplication {
        application {
            module()
        }

        val response = client.post("/api/v1/analyze") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "fastaContent": ">seq_orf_demo\nATGAAATAG\nCCCATGCCCTAA\nATGTTTTGA"
                }
                """.trimIndent()
            )
        }

        val body = response.bodyAsText()

        assertEquals(HttpStatusCode.OK, response.status)
        assertContains(body, "\"header\": \"seq_orf_demo\"")
        assertContains(body, "\"aminoAcidSequence\": \"MK\"")
        assertContains(body, "\"aminoAcidSequence\": \"MP\"")
        assertContains(body, "\"aminoAcidSequence\": \"MF\"")
    }
}
