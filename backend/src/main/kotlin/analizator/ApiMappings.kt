package analizator

import analizator.dto.AnalysisSummaryDto
import analizator.dto.AnalyzeResponseDto
import analizator.dto.OrfDto
import analizator.dto.ProteinDto
import analizator.dto.SequenceStatsDto

fun SequenceReport.toDto(): AnalyzeResponseDto {
    return AnalyzeResponseDto(
        experimentId = requireNotNull(experimentId) { "experimentId не должен быть null" },
        header = header,
        sequence = sequence,
        stats = SequenceStatsDto(
            length = stats.length,
            aCount = stats.aCount,
            tCount = stats.tCount,
            gCount = stats.gCount,
            cCount = stats.cCount,
            gcPercent = stats.gcPercent
        ),
        orfs = orfs.map {
            OrfDto(
                frame = it.frame,
                start = it.start,
                end = it.end,
                length = it.length,
                sequence = it.sequence
            )
        },
        proteins = proteins.map {
            ProteinDto(
                frame = it.frame,
                start = it.start,
                end = it.end,
                aminoAcidSequence = it.aminoAcidSequence
            )
        }
    )
}

fun AnalysisSummary.toDto(): AnalysisSummaryDto {
    return AnalysisSummaryDto(
        experimentId = experimentId,
        header = header,
        sequenceLength = sequenceLength,
        gcPercent = gcPercent,
        orfCount = orfCount,
        createdAtEpochMs = createdAtEpochMs
    )
}