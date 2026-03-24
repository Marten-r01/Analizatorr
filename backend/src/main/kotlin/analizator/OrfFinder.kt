package analizator

class OrfFinder {
    private val startCodon = "ATG"
    private val stopCodons = setOf("TAA", "TAG", "TGA")

    fun find(sequence: String): List<Orf> {
        require(sequence.isNotEmpty()) { "Последовательность пустая" }

        val result = mutableListOf<Orf>()

        for (frame in 0..2) {
            var i = frame

            while (i <= sequence.length - 3) {
                val codon = sequence.substring(i, i + 3)

                if (codon == startCodon) {
                    var j = i + 3

                    while (j <= sequence.length - 3) {
                        val stopCodon = sequence.substring(j, j + 3)

                        if (stopCodon in stopCodons) {
                            val endExclusive = j + 3
                            val orfSequence = sequence.substring(i, endExclusive)

                            result.add(
                                Orf(
                                    frame = frame,
                                    start = i + 1,
                                    end = endExclusive,
                                    length = endExclusive - i,
                                    sequence = orfSequence
                                )
                            )
                            break
                        }

                        j += 3
                    }
                }

                i += 3
            }
        }

        return result.sortedWith(compareBy<Orf> { it.frame }.thenBy { it.start })
    }
}
