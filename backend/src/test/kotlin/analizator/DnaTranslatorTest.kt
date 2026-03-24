package analizator

import kotlin.test.Test
import kotlin.test.assertEquals

class DnaTranslatorTest {
    private val translator = DnaTranslator()

    @Test
    fun translateSequenceUntilStopCodon() {
        val result = translator.translateSequence("ATGAAATAG")
        assertEquals("MK", result)
    }

    @Test
    fun translateAllOrfs() {
        val orfs = listOf(
            Orf(frame = 0, start = 1, end = 9, length = 9, sequence = "ATGAAATAG"),
            Orf(frame = 0, start = 13, end = 21, length = 9, sequence = "ATGCCCTAA"),
            Orf(frame = 0, start = 22, end = 30, length = 9, sequence = "ATGTTTTGA")
        )

        val proteins = translator.translate(orfs)

        assertEquals(listOf("MK", "MP", "MF"), proteins.map { it.aminoAcidSequence })
    }
}
