package analizator

class OrfFinder {
    fun find(sequence: String): List<Orf> {
        require(sequence.isNotEmpty()) { "Последовательность пустая" }

        val result = mutableListOf<Orf>()

        for (frame in 0..2) {
            val codonCount = codonCount(sequence.length, frame)
            if (codonCount == 0) continue

            val nearestStopAfter = IntArray(codonCount) { -1 }
            var nearestStopEnd = -1

            for (codonIndex in codonCount - 1 downTo 0) {
                val position = frame + codonIndex * 3
                nearestStopAfter[codonIndex] = nearestStopEnd

                if (isStopCodon(sequence, position)) {
                    nearestStopEnd = position + 3
                }
            }

            for (codonIndex in 0 until codonCount) {
                val position = frame + codonIndex * 3
                if (isStartCodon(sequence, position)) {
                    val endExclusive = nearestStopAfter[codonIndex]
                    if (endExclusive != -1) {
                        result.add(
                            Orf(
                                frame = frame,
                                start = position + 1,
                                end = endExclusive,
                                length = endExclusive - position,
                                sequence = sequence.substring(position, endExclusive)
                            )
                        )
                    }
                }
            }
        }

        return result
    }

    private fun codonCount(sequenceLength: Int, frame: Int): Int {
        return if (frame > sequenceLength - 3) {
            0
        } else {
            ((sequenceLength - 3 - frame) / 3) + 1
        }
    }

    private fun isStartCodon(sequence: String, position: Int): Boolean {
        return sequence[position] == 'A' &&
            sequence[position + 1] == 'T' &&
            sequence[position + 2] == 'G'
    }

    private fun isStopCodon(sequence: String, position: Int): Boolean {
        return sequence[position] == 'T' &&
            (
                sequence[position + 1] == 'A' && sequence[position + 2] == 'A' ||
                    sequence[position + 1] == 'A' && sequence[position + 2] == 'G' ||
                    sequence[position + 1] == 'G' && sequence[position + 2] == 'A'
                )
    }
}
