package analizator

class DnaTranslator {
    private val codonTable = mapOf(
        "TTT" to "F", "TTC" to "F", "TTA" to "L", "TTG" to "L",
        "TCT" to "S", "TCC" to "S", "TCA" to "S", "TCG" to "S",
        "TAT" to "Y", "TAC" to "Y", "TAA" to "*", "TAG" to "*",
        "TGT" to "C", "TGC" to "C", "TGA" to "*", "TGG" to "W",
        "CTT" to "L", "CTC" to "L", "CTA" to "L", "CTG" to "L",
        "CCT" to "P", "CCC" to "P", "CCA" to "P", "CCG" to "P",
        "CAT" to "H", "CAC" to "H", "CAA" to "Q", "CAG" to "Q",
        "CGT" to "R", "CGC" to "R", "CGA" to "R", "CGG" to "R",
        "ATT" to "I", "ATC" to "I", "ATA" to "I", "ATG" to "M",
        "ACT" to "T", "ACC" to "T", "ACA" to "T", "ACG" to "T",
        "AAT" to "N", "AAC" to "N", "AAA" to "K", "AAG" to "K",
        "AGT" to "S", "AGC" to "S", "AGA" to "R", "AGG" to "R",
        "GTT" to "V", "GTC" to "V", "GTA" to "V", "GTG" to "V",
        "GCT" to "A", "GCC" to "A", "GCA" to "A", "GCG" to "A",
        "GAT" to "D", "GAC" to "D", "GAA" to "E", "GAG" to "E",
        "GGT" to "G", "GGC" to "G", "GGA" to "G", "GGG" to "G"
    )

    fun translate(orfs: List<Orf>): List<ProteinTranslation> {
        return orfs.map { orf ->
            ProteinTranslation(
                frame = orf.frame,
                start = orf.start,
                end = orf.end,
                aminoAcidSequence = translateSequence(orf.sequence)
            )
        }
    }

    fun translateSequence(sequence: String): String {
        require(sequence.isNotEmpty()) { "Последовательность для трансляции пустая" }
        require(sequence.length % 3 == 0) { "Длина последовательности для трансляции должна быть кратна 3" }

        val result = StringBuilder()

        for (i in sequence.indices step 3) {
            val codon = sequence.substring(i, i + 3)
            val aminoAcid = requireNotNull(codonTable[codon]) { "Неизвестный кодон: $codon" }

            if (aminoAcid == "*") {
                break
            }

            result.append(aminoAcid)
        }

        return result.toString()
    }
}
