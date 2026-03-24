package analizator.dto

import kotlinx.serialization.Serializable

@Serializable
data class HealthResponseDto(
    val status: String
)
