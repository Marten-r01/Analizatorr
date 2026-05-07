package analizator

class FastaParser(
    private val validator: FastaValidator
) {
    fun parse(lines: List<String>): FastaRecord {
        val normalizedLines = lines
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        validator.validateStructure(normalizedLines)

        val header = normalizedLines.first().removePrefix(">").trim()
        val sequence = buildString {
            normalizedLines
                .drop(1)
                .takeWhile { !it.startsWith(">") }
                .forEach { line ->
                    line.forEach { nucleotide ->
                        val normalized = nucleotide.uppercaseChar()
                        if (normalized != 'N') {
                            append(normalized)
                        }
                    }
                }
        }

        validator.validateHeader(header)
        validator.validateSequence(sequence)

        return FastaRecord(
            header = header,
            sequence = sequence
        )
    }
}
