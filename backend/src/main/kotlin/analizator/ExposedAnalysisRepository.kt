package analizator

import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class ExposedAnalysisRepository : AnalysisRepository {
    override fun save(report: SequenceReport, originalFileName: String?): SequenceReport {
        return transaction {
            val experimentId = ExperimentsTable.insertAndGetId {
                it[ExperimentsTable.originalFileName] = originalFileName
                it[status] = "COMPLETED"
                it[createdAtEpochMs] = System.currentTimeMillis()
            }

            val sequenceId = SequencesTable.insertAndGetId {
                it[SequencesTable.experimentId] = experimentId
                it[header] = report.header.take(512)
                it[rawSequence] = report.sequence
                it[normalizedSequence] = report.sequence
                it[length] = report.stats.length
                it[gcContent] = report.stats.gcPercent
            }

            report.orfs.forEachIndexed { index, orf ->
                val orfId = OrfsTable.insertAndGetId {
                    it[OrfsTable.sequenceId] = sequenceId
                    it[frame] = orf.frame
                    it[startPos] = orf.start
                    it[endPos] = orf.end
                    it[length] = orf.length
                    it[sequence] = orf.sequence
                }

                val protein = report.proteins.getOrNull(index)
                if (protein != null) {
                    ProteinsTable.insert {
                        it[ProteinsTable.orfId] = orfId
                        it[aminoAcidSequence] = protein.aminoAcidSequence
                    }
                }
            }

            report.copy(experimentId = experimentId.value)
        }
    }

    override fun findByExperimentId(experimentId: Int): SequenceReport? {
        return transaction {
            val sequenceRow = SequencesTable
                .selectAll()
                .where { SequencesTable.experimentId eq experimentId }
                .singleOrNull()
                ?: return@transaction null

            val sequenceId = sequenceRow[SequencesTable.id].value

            val stats = SequenceStats(
                length = sequenceRow[SequencesTable.length],
                aCount = sequenceRow[SequencesTable.normalizedSequence].count { it == 'A' },
                tCount = sequenceRow[SequencesTable.normalizedSequence].count { it == 'T' },
                gCount = sequenceRow[SequencesTable.normalizedSequence].count { it == 'G' },
                cCount = sequenceRow[SequencesTable.normalizedSequence].count { it == 'C' },
                gcPercent = sequenceRow[SequencesTable.gcContent]
            )

            val orfRows = OrfsTable
                .selectAll()
                .where { OrfsTable.sequenceId eq sequenceId }
                .orderBy(OrfsTable.frame to SortOrder.ASC, OrfsTable.startPos to SortOrder.ASC)
                .toList()

            val orfs = orfRows.map { row ->
                Orf(
                    frame = row[OrfsTable.frame],
                    start = row[OrfsTable.startPos],
                    end = row[OrfsTable.endPos],
                    length = row[OrfsTable.length],
                    sequence = row[OrfsTable.sequence]
                )
            }

            val proteins = orfRows.mapNotNull { orfRow ->
                val orfId = orfRow[OrfsTable.id].value
                val proteinRow = ProteinsTable
                    .selectAll()
                    .where { ProteinsTable.orfId eq orfId }
                    .singleOrNull()

                if (proteinRow == null) {
                    null
                } else {
                    ProteinTranslation(
                        frame = orfRow[OrfsTable.frame],
                        start = orfRow[OrfsTable.startPos],
                        end = orfRow[OrfsTable.endPos],
                        aminoAcidSequence = proteinRow[ProteinsTable.aminoAcidSequence]
                    )
                }
            }

            SequenceReport(
                experimentId = experimentId,
                header = sequenceRow[SequencesTable.header],
                sequence = sequenceRow[SequencesTable.normalizedSequence],
                stats = stats,
                orfs = orfs,
                proteins = proteins
            )
        }
    }
}
