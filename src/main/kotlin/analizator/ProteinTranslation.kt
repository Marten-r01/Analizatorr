package analizator

data class ProteinTranslation(
    val frame: Int,
    val start: Int,
    val end: Int,
    val aminoAcidSequence: String
)
