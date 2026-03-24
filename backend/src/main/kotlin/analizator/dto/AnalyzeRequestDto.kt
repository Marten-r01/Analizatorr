package analizator.dto

import kotlinx.serialization.Serializable

@Serializable
data class AnalyzeRequestDto(
    val fastaContent: String,
    val originalFileName: String? = null
)
