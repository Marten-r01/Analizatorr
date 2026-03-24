package analizator

import kotlin.test.Test
import kotlin.test.assertEquals

class SequenceAnalysisServiceTest {
    private val service = SequenceAnalysisService(
        parser = FastaParser(FastaValidator()),
        gcAnalyzer = GcAnalyzer(),
        orfFinder = OrfFinder(),
        dnaTranslator = DnaTranslator()
    )

    @Test
    fun analyzeSequenceAndReturnProteins() {
        val lines = listOf(
            ">seq_orf_demo",
            "ATGAAATAG",
            "CCCATGCCCTAA",
            "ATGTTTTGA"
        )

        val report = service.analyze(lines)

        assertEquals("seq_orf_demo", report.header)
        assertEquals(30, report.stats.length)
        assertEquals(3, report.orfs.size)
        assertEquals(listOf("MK", "MP", "MF"), report.proteins.map { it.aminoAcidSequence })
    }
}
