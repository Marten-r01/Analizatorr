package analizator

import org.jetbrains.exposed.v1.core.dao.id.IntIdTable

object OrfsTable : IntIdTable("orfs") {
    val sequenceId = reference("sequence_id", SequencesTable)
    val frame = integer("frame")
    val startPos = integer("start_pos")
    val endPos = integer("end_pos")
    val length = integer("length")
    val sequence = text("sequence")
}
