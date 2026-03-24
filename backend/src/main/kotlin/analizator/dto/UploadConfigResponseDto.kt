package analizator.dto

import kotlinx.serialization.Serializable

@Serializable
data class UploadConfigResponseDto(
    val maxFileSizeBytes: Int,
    val maxFileSizeMb: Int,
    val fileFieldName: String,
    val acceptedRequestContentType: String
)
