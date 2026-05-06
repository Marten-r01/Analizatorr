@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package analizator.frontend

import analizator.dto.AnalysisSummaryDto
import analizator.dto.AnalyzeResponseDto
import analizator.dto.ErrorResponseDto
import analizator.dto.UploadConfigResponseDto
import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.js.JsString
import org.w3c.fetch.RequestInit
import org.w3c.fetch.Response
import org.w3c.files.File
import org.w3c.xhr.FormData

@Suppress("UNUSED_PARAMETER")
private fun postFormDataRequestInit(body: FormData): RequestInit =
    js("({ method: 'POST', body: body })")

object ApiClient {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    suspend fun getUploadConfig(baseUrl: String): UploadConfigResponseDto {
        val response: Response = window.fetch("$baseUrl/api/v1/upload-config").await()
        return decode<UploadConfigResponseDto>(response)
    }

    suspend fun uploadFasta(baseUrl: String, file: File): AnalyzeResponseDto {
        val formData = FormData()
        formData.append("file", file, file.name)

        val response: Response = window.fetch(
            "$baseUrl/api/v1/analyze-upload",
            postFormDataRequestInit(formData)
        ).await()

        return decode<AnalyzeResponseDto>(response)
    }

    suspend fun getAnalysisById(baseUrl: String, experimentId: Int): AnalyzeResponseDto {
        val response: Response = window.fetch("$baseUrl/api/v1/analysis/$experimentId").await()
        return decode<AnalyzeResponseDto>(response)
    }

    suspend fun getLatestAnalyses(baseUrl: String, limit: Int): List<AnalysisSummaryDto> {
        val response: Response = window.fetch("$baseUrl/api/v1/analyses?limit=$limit").await()
        return decode<List<AnalysisSummaryDto>>(response)
    }

    private suspend inline fun <reified T> decode(response: Response): T {
        val text = response.text().await<JsString>().toString()
        val isSuccessful = response.status.toInt() in 200..299
        if (!isSuccessful) {
            throw IllegalStateException(extractErrorMessage(text))
        }
        return json.decodeFromString<T>(text)
    }

    private fun extractErrorMessage(text: String): String {
        return runCatching {
            json.decodeFromString<ErrorResponseDto>(text).message
        }.getOrElse {
            text.ifBlank { "Ошибка запроса к backend" }
        }
    }
}
