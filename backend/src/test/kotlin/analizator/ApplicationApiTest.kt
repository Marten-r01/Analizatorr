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
    fun uploadConfigEndpointReturnsExpectedLimit() = testApplication {
        application {
            module(repository = ExposedAnalysisRepository())
        }

        val response = client.get("/api/v1/upload-config")
        val body = response.bodyAsText()

        assertEquals(HttpStatusCode.OK, response.status)
        assertContains(body, "\"maxFileSizeMb\": 10")
        assertContains(body, "\"fileFieldName\": \"file\"")
    }

    @Test
    fun multipartUploadPersistsAnalysisAndReturnsId() = testApplication {
        application {
            module(repository = ExposedAnalysisRepository())
        }

        val fastaContent = """
            >seq_orf_demo
            ATGAAATAG
            CCCATGCCCTAA
            ATGTTTTGA
        """.trimIndent()

        val response = client.post("/api/v1/analyze-upload") {
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append(
                            UploadConstraints.FILE_FIELD_NAME,
                            fastaContent.toByteArray(),
                            Headers.build {
                                append(HttpHeaders.ContentDisposition, "filename=\"sample.fasta\"")
                                append(HttpHeaders.ContentType, "text/plain")
                            }
                        )
                    }
                )
            )
        }

        val body = response.bodyAsText()

        assertEquals(HttpStatusCode.OK, response.status)
        assertContains(body, "\"experimentId\": 1")
        assertContains(body, "\"header\": \"seq_orf_demo\"")
        assertContains(body, "\"aminoAcidSequence\": \"MK\"")
        assertContains(body, "\"aminoAcidSequence\": \"MP\"")
        assertContains(body, "\"aminoAcidSequence\": \"MF\"")
    }

    @Test
    fun oversizedMultipartUploadReturns413() = testApplication {
        application {
            module(repository = ExposedAnalysisRepository())
        }

        val oversizedFasta = buildString {
            append(">big\n")
            append("A".repeat(UploadConstraints.MAX_FASTA_SIZE_BYTES + 1))
        }

        val response = client.post("/api/v1/analyze-upload") {
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append(
                            UploadConstraints.FILE_FIELD_NAME,
                            oversizedFasta.toByteArray(),
                            Headers.build {
                                append(HttpHeaders.ContentDisposition, "filename=\"oversized.fasta\"")
                                append(HttpHeaders.ContentType, "text/plain")
                            }
                        )
                    }
                )
            )
        }

        assertEquals(HttpStatusCode.PayloadTooLarge, response.status)
        assertContains(
            response.bodyAsText(),
            "\"message\": \"Размер загружаемого FASTA-файла превышает 10 МБ\""
        )
    }

    @Test
    fun multipartUploadWithoutFileReturns400() = testApplication {
        application {
            module(repository = ExposedAnalysisRepository())
        }

        val response = client.post("/api/v1/analyze-upload") {
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append("description", "missing file")
                    }
                )
            )
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertContains(
            response.bodyAsText(),
            "\"message\": \"В multipart-запросе отсутствует файл в поле 'file'\""
        )
    }

    @Test
    fun getSavedAnalysisByIdAfterMultipartUpload() = testApplication {
        application {
            module(repository = ExposedAnalysisRepository())
        }

        val fastaContent = """
            >seq_orf_demo
            ATGAAATAG
            CCCATGCCCTAA
            ATGTTTTGA
        """.trimIndent()

        client.post("/api/v1/analyze-upload") {
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append(
                            UploadConstraints.FILE_FIELD_NAME,
                            fastaContent.toByteArray(),
                            Headers.build {
                                append(HttpHeaders.ContentDisposition, "filename=\"sample.fasta\"")
                                append(HttpHeaders.ContentType, "text/plain")
                            }
                        )
                    }
                )
            )
        }

        val response = client.get("/api/v1/analysis/1")
        val body = response.bodyAsText()

        assertEquals(HttpStatusCode.OK, response.status)
        assertContains(body, "\"experimentId\": 1")
        assertContains(body, "\"sequence\": \"ATGAAATAGCCCATGCCCTAAATGTTTTGA\"")
        assertContains(body, "\"aminoAcidSequence\": \"MF\"")
    }
}
