package analizator

class SequenceAnalysisService(
    private val parser: FastaParser,
    private val gcAnalyzer: GcAnalyzer,
    private val orfFinder: OrfFinder,
    private val dnaTranslator: DnaTranslator
) {
    fun analyze(lines: List<String>): SequenceReport {
        val record = parser.parse(lines)
        val stats = gcAnalyzer.analyze(record.sequence)
        val orfs = orfFinder.find(record.sequence)
        val proteins = dnaTranslator.translate(orfs)

        return SequenceReport(
            header = record.header,
            sequence = record.sequence,
            stats = stats,
            orfs = orfs,
            proteins = proteins
        )
    }
}
