package analizator

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FastaParserTest {
    private val parser = FastaParser(FastaValidator())

    @Test
    fun parseMultilineFastaAndUppercaseSequence() {
        val lines = listOf(
            ">seq_1",
            "atgc",
            "ggta"
        )

        val record = parser.parse(lines)

        assertEquals("seq_1", record.header)
        assertEquals("ATGCGGTA", record.sequence)
    }

    @Test
    fun failWhenSequenceContainsInvalidSymbol() {
        val lines = listOf(
            ">seq_1",
            "ATGZ"
        )

        assertFailsWith<IllegalArgumentException> {
            parser.parse(lines)
        }
    }
}
