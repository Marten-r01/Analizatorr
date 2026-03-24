package analizator

import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class ApplicationApiTest {
    @BeforeTest
    fun setup() {
        DatabaseFactory.connect(
            url = "jdbc:h2:mem:api_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver",
            user = "",
            password = ""
        )
        DatabaseFactory.resetSchema()
    }

    @Test
    fun analyzeEndpointPersistsAndReturnsId() = testApplication {
        application {
            module(repository = ExposedAnalysisRepository())
        }

        val response = client.post("/api/v1/analyze") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "fastaContent": ">seq_orf_demo\nATGAAATAG\nCCCATGCCCTAA\nATGTTTTGA",
                  "originalFileName": "sample.fasta"
                }
                """.trimIndent()
            )
        }

        val body = response.bodyAsText()

        assertEquals(HttpStatusCode.OK, response.status)
        assertContains(body, "\"experimentId\": 1")
        assertContains(body, "\"header\": \"seq_orf_demo\"")
        assertContains(body, "\"aminoAcidSequence\": \"MK\"")
    }

    @Test
    fun getSavedAnalysisById() = testApplication {
        application {
            module(repository = ExposedAnalysisRepository())
        }

        client.post("/api/v1/analyze") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "fastaContent": ">seq_orf_demo\nATGAAATAG\nCCCATGCCCTAA\nATGTTTTGA",
                  "originalFileName": "sample.fasta"
                }
                """.trimIndent()
            )
        }

        val response = client.get("/api/v1/analysis/1")
        val body = response.bodyAsText()

        assertEquals(HttpStatusCode.OK, response.status)
        assertContains(body, "\"experimentId\": 1")
        assertContains(body, "\"sequence\": \"ATGAAATAGCCCATGCCCTAAATGTTTTGA\"")
        assertContains(body, "\"aminoAcidSequence\": \"MF\"")
    }

    @Test
    fun getMissingAnalysisReturns404() = testApplication {
        application {
            module(repository = ExposedAnalysisRepository())
        }

        val response = client.get("/api/v1/analysis/999")

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertContains(response.bodyAsText(), "\"message\": \"Анализ не найден\"")
    }
}
