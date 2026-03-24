package analizator

import org.jetbrains.exposed.v1.core.dao.id.IntIdTable

object SequencesTable : IntIdTable("sequences") {
    val experimentId = reference("experiment_id", ExperimentsTable)
    val header = varchar("header", 512)
    val rawSequence = text("raw_sequence")
    val normalizedSequence = text("normalized_sequence")
    val length = integer("length")
    val gcContent = double("gc_content")
}
