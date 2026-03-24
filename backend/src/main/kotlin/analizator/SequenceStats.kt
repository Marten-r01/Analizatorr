package analizator

data class SequenceStats(
    val length: Int,
    val aCount: Int,
    val tCount: Int,
    val gCount: Int,
    val cCount: Int,
    val gcPercent: Double
)
