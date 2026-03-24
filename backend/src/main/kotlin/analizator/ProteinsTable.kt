package analizator

import org.jetbrains.exposed.v1.core.dao.id.IntIdTable

object ProteinsTable : IntIdTable("proteins") {
    val orfId = reference("orf_id", OrfsTable)
    val aminoAcidSequence = text("amino_acid_sequence")
}
