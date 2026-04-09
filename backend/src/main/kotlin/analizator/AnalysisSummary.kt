package analizator

data class AnalysisSummary(
    val experimentId: Int,
    val header: String,
    val sequenceLength: Int,
    val gcPercent: Double,
    val orfCount: Int,
    val createdAtEpochMs: Long
)