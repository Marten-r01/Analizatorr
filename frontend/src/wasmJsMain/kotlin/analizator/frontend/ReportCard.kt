package analizator.frontend

import analizator.dto.AnalyzeResponseDto
import analizator.dto.OrfDto
import analizator.dto.ProteinDto
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ReportCard(report: AnalyzeResponseDto) {
    var selectedOrfIndex by remember(report.experimentId) { mutableStateOf<Int?>(null) }
    val selectedOrf = selectedOrfIndex?.let(report.orfs::getOrNull)

    SectionCard(title = "Результат анализа") {
        Text("experimentId=${report.experimentId}")
        Text("header=${report.header}")
        Text("Длина последовательности=${report.stats.length}")
        Text("GC=${formatDouble(report.stats.gcPercent)}%")

        ResultBreakdown(report)

        Text(
            text = "График процентного содержания нуклеотидов",
            style = MaterialTheme.typography.titleMedium
        )
        SequenceLegend()
        NucleotideDistributionChart(report.stats)

        Text(
            text = "Последовательность и найденные ORF",
            style = MaterialTheme.typography.titleMedium
        )
        ColorizedSequence(
            sequence = report.sequence,
            selectedOrf = selectedOrf
        )

        OrfList(
            orfs = report.orfs,
            selectedOrfIndex = selectedOrfIndex,
            onSelect = { index ->
                selectedOrfIndex = if (selectedOrfIndex == index) null else index
            }
        )

        ProteinList(report.proteins)
    }
}

@Composable
private fun ResultBreakdown(report: AnalyzeResponseDto) {
    val stats = report.stats
    val total = stats.length.coerceAtLeast(1)
    val atPercent = 100.0 - stats.gcPercent
    val longestOrf = report.orfs.maxByOrNull { it.length }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "Роспись результатов",
            style = MaterialTheme.typography.titleMedium
        )
        Text("A=${stats.aCount} (${formatPercent(stats.aCount, total)}%)")
        Text("T=${stats.tCount} (${formatPercent(stats.tCount, total)}%)")
        Text("G=${stats.gCount} (${formatPercent(stats.gCount, total)}%)")
        Text("C=${stats.cCount} (${formatPercent(stats.cCount, total)}%)")
        Text("AT=${formatDouble(atPercent)}%, GC=${formatDouble(stats.gcPercent)}%")
        Text("Найдено ORF: ${report.orfs.size}")
        Text("Белковых трансляций: ${report.proteins.size}")
        if (longestOrf == null) {
            Text("Самый длинный ORF: не найден")
        } else {
            Text(
                "Самый длинный ORF: frame=${longestOrf.frame}, start=${longestOrf.start}, " +
                    "end=${longestOrf.end}, length=${longestOrf.length}"
            )
        }
    }
}

@Composable
private fun OrfList(
    orfs: List<OrfDto>,
    selectedOrfIndex: Int?,
    onSelect: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "ORF (${orfs.size})",
            style = MaterialTheme.typography.titleMedium
        )

        if (orfs.isEmpty()) {
            Text("ORF не найдены")
        } else {
            orfs.forEachIndexed { index, orf ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("frame=${orf.frame}, start=${orf.start}, end=${orf.end}, length=${orf.length}")
                        Text(orf.sequence)

                        TextButton(onClick = { onSelect(index) }) {
                            if (selectedOrfIndex == index) {
                                Text("Снять выделение")
                            } else {
                                Text("Подсветить")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProteinList(proteins: List<ProteinDto>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Белковые последовательности (${proteins.size})",
            style = MaterialTheme.typography.titleMedium
        )

        if (proteins.isEmpty()) {
            Text("Трансляции отсутствуют")
        } else {
            proteins.forEach { protein ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("frame=${protein.frame}")
                            Text("start=${protein.start}")
                            Text("end=${protein.end}")
                            Text("length=${protein.aminoAcidSequence.length}")
                        }
                        Text(protein.aminoAcidSequence)
                    }
                }
            }
        }
    }
}

private fun formatPercent(count: Int, total: Int): String {
    return formatDouble(count.toDouble() * 100.0 / total.toDouble())
}
