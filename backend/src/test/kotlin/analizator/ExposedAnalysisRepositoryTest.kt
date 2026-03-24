package analizator

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ExposedAnalysisRepositoryTest {
    private lateinit var repository: AnalysisRepository

    @BeforeTest
    fun setup() {
        DatabaseFactory.connect(
            url = "jdbc:h2:mem:repo_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver",
            user = "",
            password = ""
        )
        DatabaseFactory.resetSchema()
        repository = ExposedAnalysisRepository()
    }

    @Test
    fun saveAndFindReportByExperimentId() {
        val report = SequenceReport(
            header = "seq_orf_demo",
            sequence = "ATGAAATAGCCCATGCCCTAAATGTTTTGA",
            stats = SequenceStats(
                length = 30,
                aCount = 10,
                tCount = 9,
                gCount = 5,
                cCount = 6,
                gcPercent = 36.67
            ),
            orfs = listOf(
                Orf(frame = 0, start = 1, end = 9, length = 9, sequence = "ATGAAATAG"),
                Orf(frame = 0, start = 13, end = 21, length = 9, sequence = "ATGCCCTAA"),
                Orf(frame = 0, start = 22, end = 30, length = 9, sequence = "ATGTTTTGA")
            ),
            proteins = listOf(
                ProteinTranslation(frame = 0, start = 1, end = 9, aminoAcidSequence = "MK"),
                ProteinTranslation(frame = 0, start = 13, end = 21, aminoAcidSequence = "MP"),
                ProteinTranslation(frame = 0, start = 22, end = 30, aminoAcidSequence = "MF")
            )
        )

        val saved = repository.save(report, "sample.fasta")
        val loaded = repository.findByExperimentId(requireNotNull(saved.experimentId))

        assertNotNull(loaded)
        assertEquals(saved.experimentId, loaded.experimentId)
        assertEquals("seq_orf_demo", loaded.header)
        assertEquals(30, loaded.stats.length)
        assertEquals(3, loaded.orfs.size)
        assertEquals(listOf("MK", "MP", "MF"), loaded.proteins.map { it.aminoAcidSequence })
    }
}
