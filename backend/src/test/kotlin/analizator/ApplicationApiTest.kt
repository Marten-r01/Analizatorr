package analizator

import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
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
    fun latestAnalysesEndpointReturnsSavedItems() = testApplication {
        application {
            module(repository = ExposedAnalysisRepository())
        }

        val fasta1 = """
            >seq_one
            ATGAAATAG
        """.trimIndent()

        val fasta2 = """
            >seq_two
            ATGCCCTAA
        """.trimIndent()

        client.post("/api/v1/analyze-upload") {
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append(
                            UploadConstraints.FILE_FIELD_NAME,
                            fasta1.toByteArray(),
                            Headers.build {
                                append(HttpHeaders.ContentDisposition, "filename=\"one.fasta\"")
                                append(HttpHeaders.ContentType, "text/plain")
                            }
                        )
                    }
                )
            )
        }

        client.post("/api/v1/analyze-upload") {
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append(
                            UploadConstraints.FILE_FIELD_NAME,
                            fasta2.toByteArray(),
                            Headers.build {
                                append(HttpHeaders.ContentDisposition, "filename=\"two.fasta\"")
                                append(HttpHeaders.ContentType, "text/plain")
                            }
                        )
                    }
                )
            )
        }

        val response = client.get("/api/v1/analyses?limit=10")
        val body = response.bodyAsText()

        assertEquals(HttpStatusCode.OK, response.status)
        assertContains(body, "\"experimentId\": 2")
        assertContains(body, "\"header\": \"seq_two\"")
        assertContains(body, "\"experimentId\": 1")
        assertContains(body, "\"header\": \"seq_one\"")
    }

    @Test
    fun latestAnalysesEndpointRejectsInvalidLimit() = testApplication {
        application {
            module(repository = ExposedAnalysisRepository())
        }

        val response = client.get("/api/v1/analyses?limit=0")

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertContains(response.bodyAsText(), "\"message\": \"limit должен быть в диапазоне от 1 до 100\"")
    }
}