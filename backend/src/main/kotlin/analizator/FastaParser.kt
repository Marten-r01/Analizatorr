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
        val sequence = normalizedLines
            .drop(1)
            .joinToString(separator = "")
            .uppercase()

        validator.validateHeader(header)
        validator.validateSequence(sequence)

        return FastaRecord(
            header = header,
            sequence = sequence
        )
    }
}
