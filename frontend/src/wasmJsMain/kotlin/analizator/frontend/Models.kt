package analizator.frontend

import kotlinx.serialization.Serializable
import org.w3c.files.File

@Serializable
data class UploadConfigResponseDto(
    val maxFileSizeBytes: Int,
    val maxFileSizeMb: Int,
    val fileFieldName: String,
    val acceptedRequestContentType: String
)

@Serializable
data class AnalyzeResponseDto(
    val experimentId: Int,
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

@Serializable
data class ErrorResponseDto(
    val message: String
)

@Serializable
data class AnalysisSummaryDto(
    val experimentId: Int,
    val header: String,
    val sequenceLength: Int,
    val gcPercent: Double,
    val orfCount: Int,
    val createdAtEpochMs: Long
)

data class SelectedFile(
    val file: File,
    val name: String,
    val sizeBytes: Long
)