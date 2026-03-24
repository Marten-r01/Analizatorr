package analizator

data class SequenceReport(
    val experimentId: Int? = null,
    val header: String,
    val sequence: String,
    val stats: SequenceStats,
    val orfs: List<Orf>,
    val proteins: List<ProteinTranslation>
    
)
