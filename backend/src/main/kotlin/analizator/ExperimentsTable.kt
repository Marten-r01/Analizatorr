package analizator

import org.jetbrains.exposed.v1.core.dao.id.IntIdTable

object ExperimentsTable : IntIdTable("experiments") {
    val originalFileName = varchar("original_file_name", 255).nullable()
    val status = varchar("status", 32)
    val createdAtEpochMs = long("created_at_epoch_ms")
}
