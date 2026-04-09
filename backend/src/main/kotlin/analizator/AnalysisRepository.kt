package analizator

interface AnalysisRepository {
    fun save(report: SequenceReport, originalFileName: String?): SequenceReport
    fun findByExperimentId(experimentId: Int): SequenceReport?
    fun findLatest(limit: Int): List<AnalysisSummary>
}