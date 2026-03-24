package analizator

class FastaValidator {
    private val allowedSymbols = setOf('A', 'T', 'G', 'C')

    fun validateStructure(lines: List<String>) {
        require(lines.isNotEmpty()) { "Файл пустой" }
        require(lines.first().startsWith(">")) { "FASTA-заголовок должен начинаться с символа '>'" }
        require(lines.count { it.startsWith(">") } == 1) { "На 4 неделе поддерживается только одна FASTA-запись" }
    }

    fun validateHeader(header: String) {
        require(header.isNotBlank()) { "Заголовок FASTA пустой" }
    }

    fun validateSequence(sequence: String) {
        require(sequence.isNotEmpty()) { "После заголовка отсутствует последовательность" }

        val invalidIndex = sequence.indexOfFirst { it !in allowedSymbols }
        require(invalidIndex == -1) {
            "Обнаружен недопустимый символ '${sequence[invalidIndex]}' в позиции ${invalidIndex + 1}"
        }
    }
}
