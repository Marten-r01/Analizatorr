package analizator

class SequenceAnalysisService(
    private val parser: FastaParser,
    private val gcAnalyzer: GcAnalyzer,
    private val orfFinder: OrfFinder,
    private val dnaTranslator: DnaTranslator,
    private val repository: AnalysisRepository
) {
    fun analyzeAndSave(lines: List<String>, originalFileName: String?): SequenceReport {
        val record = parser.parse(lines)
        val stats = gcAnalyzer.analyze(record.sequence)
        val orfs = orfFinder.find(record.sequence)
        val proteins = dnaTranslator.translate(orfs)

        val report = SequenceReport(
            header = record.header,
            sequence = record.sequence,
            stats = stats,
            orfs = orfs,
            proteins = proteins
        )

        return repository.save(report, originalFileName)
    }

    fun getByExperimentId(experimentId: Int): SequenceReport? {
        return repository.findByExperimentId(experimentId)
    }
}
