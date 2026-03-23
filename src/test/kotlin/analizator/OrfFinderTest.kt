package analizator

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OrfFinderTest {
    private val orfFinder = OrfFinder()

    @Test
    fun findThreeOrfsInFrameZero() {
        val orfs = orfFinder.find("ATGAAATAGCCCATGCCCTAAATGTTTTGA")

        assertEquals(3, orfs.size)

        assertEquals(0, orfs[0].frame)
        assertEquals(1, orfs[0].start)
        assertEquals(9, orfs[0].end)
        assertEquals("ATGAAATAG", orfs[0].sequence)

        assertEquals(0, orfs[1].frame)
        assertEquals(13, orfs[1].start)
        assertEquals(21, orfs[1].end)
        assertEquals("ATGCCCTAA", orfs[1].sequence)

        assertEquals(0, orfs[2].frame)
        assertEquals(22, orfs[2].start)
        assertEquals(30, orfs[2].end)
        assertEquals("ATGTTTTGA", orfs[2].sequence)
    }

    @Test
    fun returnEmptyListWhenNoOrfExists() {
        val orfs = orfFinder.find("AAACCCGGGTTT")

        assertTrue(orfs.isEmpty())
    }
}
