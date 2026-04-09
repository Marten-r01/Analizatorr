package analizator.frontend

import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.w3c.fetch.RequestInit
import org.w3c.fetch.Response
import org.w3c.files.File
import org.w3c.xhr.FormData

object ApiClient {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    suspend fun getUploadConfig(baseUrl: String): UploadConfigResponseDto {
        val response = window.fetch("$baseUrl/api/v1/upload-config").await()
        return decode(response)
    }

    suspend fun uploadFasta(baseUrl: String, file: File): AnalyzeResponseDto {
        val formData = FormData()
        formData.append("file", file, file.name)

        val response = window.fetch(
            "$baseUrl/api/v1/analyze-upload",
            RequestInit(
                method = "POST",
                body = formData
            )
        ).await()

        return decode(response)
    }

    suspend fun getAnalysisById(baseUrl: String, experimentId: Int): AnalyzeResponseDto {
        val response = window.fetch("$baseUrl/api/v1/analysis/$experimentId").await()
        return decode(response)
    }

    suspend fun getLatestAnalyses(baseUrl: String, limit: Int): List<AnalysisSummaryDto> {
        val response = window.fetch("$baseUrl/api/v1/analyses?limit=$limit").await()
        return decode(response)
    }

    suspend inline fun <reified T> decode(response: Response): T {
        val text = response.text().await()
        if (!response.ok) {
            throw IllegalStateException(extractErrorMessage(text))
        }
        return json.decodeFromString(text)
    }

    private fun extractErrorMessage(text: String): String {
        return runCatching {
            json.decodeFromString<ErrorResponseDto>(text).message
        }.getOrElse {
            text.ifBlank { "Ошибка запроса к backend" }
        }
    }
}