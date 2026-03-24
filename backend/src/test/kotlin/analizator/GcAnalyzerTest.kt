package analizator

import kotlin.test.Test
import kotlin.test.assertEquals

class GcAnalyzerTest {
    private val analyzer = GcAnalyzer()

    @Test
    fun calculateStatsCorrectly() {
        val stats = analyzer.analyze("ATGAAATAGCCCATGCCCTAAATGTTTTGA")

        assertEquals(30, stats.length)
        assertEquals(10, stats.aCount)
        assertEquals(9, stats.tCount)
        assertEquals(5, stats.gCount)
        assertEquals(6, stats.cCount)
        assertEquals(36.67, stats.gcPercent)
    }
}
