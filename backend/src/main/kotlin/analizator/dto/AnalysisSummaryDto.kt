package analizator.dto

import kotlinx.serialization.Serializable

@Serializable
data class AnalysisSummaryDto(
    val experimentId: Int,
    val header: String,
    val sequenceLength: Int,
    val gcPercent: Double,
    val orfCount: Int,
    val createdAtEpochMs: Long
)