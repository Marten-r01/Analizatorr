package analizator

class SequenceAnalysisService(
    private val parser: FastaParser,
    private val gcAnalyzer: GcAnalyzer,
    private val orfFinder: OrfFinder
) {
    fun analyze(lines: List<String>): SequenceReport {
        val record = parser.parse(lines)
        val stats = gcAnalyzer.analyze(record.sequence)
        val orfs = orfFinder.find(record.sequence)

        return SequenceReport(
            header = record.header,
            sequence = record.sequence,
            stats = stats,
            orfs = orfs
        )
    }
}
