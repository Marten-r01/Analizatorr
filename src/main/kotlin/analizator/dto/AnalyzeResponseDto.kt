package analizator.dto

import kotlinx.serialization.Serializable

@Serializable
data class AnalyzeResponseDto(
    val header: String,
    val sequence: String,
    val stats: SequenceStatsDto,
    val orfs: List<OrfDto>,
    val proteins: List<ProteinDto>
)

@Serializable
data class SequenceStatsDto(
    val length: Int,
    val aCount: Int,
    val tCount: Int,
    val gCount: Int,
    val cCount: Int,
    val gcPercent: Double
)

@Serializable
data class OrfDto(
    val frame: Int,
    val start: Int,
    val end: Int,
    val length: Int,
    val sequence: String
)

@Serializable
data class ProteinDto(
    val frame: Int,
    val start: Int,
    val end: Int,
    val aminoAcidSequence: String
)
