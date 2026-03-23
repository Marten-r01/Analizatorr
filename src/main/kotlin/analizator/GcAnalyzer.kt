package analizator

import java.math.BigDecimal
import java.math.RoundingMode

class GcAnalyzer {
    fun analyze(sequence: String): SequenceStats {
        require(sequence.isNotEmpty()) { "Последовательность пустая" }

        val aCount = sequence.count { it == 'A' }
        val tCount = sequence.count { it == 'T' }
        val gCount = sequence.count { it == 'G' }
        val cCount = sequence.count { it == 'C' }
        val length = sequence.length
        val gcPercent = BigDecimal((gCount + cCount) * 100.0 / length)
            .setScale(2, RoundingMode.HALF_UP)
            .toDouble()

        return SequenceStats(
            length = length,
            aCount = aCount,
            tCount = tCount,
            gCount = gCount,
            cCount = cCount,
            gcPercent = gcPercent
        )
    }
}
