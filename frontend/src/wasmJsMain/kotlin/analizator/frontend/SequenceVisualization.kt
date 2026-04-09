package analizator.frontend

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlin.math.round

private val colorA = Color(0xFF2E7D32)
private val colorT = Color(0xFFC62828)
private val colorG = Color(0xFF1565C0)
private val colorC = Color(0xFFF9A825)
private val colorOther = Color(0xFF616161)
private val selectedOrfBackground = Color(0xFFFFF59D)

@Composable
fun SequenceLegend() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LegendChip("A", colorA)
        LegendChip("T", colorT)
        LegendChip("G", colorG)
        LegendChip("C", colorC)
    }
}

@Composable
fun NucleotideDistributionChart(stats: SequenceStatsDto) {
    val total = if (stats.length == 0) 1 else stats.length

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ChartRow("A", stats.aCount, total, colorA)
        ChartRow("T", stats.tCount, total, colorT)
        ChartRow("G", stats.gCount, total, colorG)
        ChartRow("C", stats.cCount, total, colorC)
    }
}

@Composable
fun ColorizedSequence(
    sequence: String,
    selectedOrf: OrfDto?
) {
    val selectedRange = selectedOrf?.let { (it.start - 1) until it.end }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (selectedOrf == null) {
                Text("Подсветка ORF не выбрана")
            } else {
                Text(
                    "Выбран ORF: frame=${selectedOrf.frame}, start=${selectedOrf.start}, end=${selectedOrf.end}, length=${selectedOrf.length}"
                )
            }

            Text(
                text = buildAnnotatedString {
                    sequence.forEachIndexed { index, nucleotide ->
                        if (index > 0 && index % 60 == 0) {
                            append("\n")
                        } else if (index > 0 && index % 10 == 0) {
                            append(" ")
                        }

                        addStyle(
                            style = SpanStyle(
                                color = nucleotideColor(nucleotide),
                                background = if (selectedRange != null && index in selectedRange) {
                                    selectedOrfBackground
                                } else {
                                    Color.Transparent
                                }
                            ),
                            start = length,
                            end = length + 1
                        )
                        append(nucleotide)
                    }
                },
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun LegendChip(
    label: String,
    color: Color
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .width(16.dp)
                .height(16.dp)
                .background(color, RoundedCornerShape(4.dp))
        )
        Text(label)
    }
}

@Composable
private fun ChartRow(
    label: String,
    count: Int,
    total: Int,
    color: Color
) {
    val ratio = count.toFloat() / total.toFloat()
    val percent = formatDouble(ratio * 100.0)

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label)
            Text("$count ($percent%)")
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(ratio.coerceIn(0f, 1f))
                    .height(18.dp)
                    .background(color, RoundedCornerShape(8.dp))
            )
        }
    }
}

private fun nucleotideColor(nucleotide: Char): Color {
    return when (nucleotide) {
        'A' -> colorA
        'T' -> colorT
        'G' -> colorG
        'C' -> colorC
        else -> colorOther
    }
}

fun formatDouble(value: Double): String {
    val rounded = round(value * 100.0) / 100.0
    val raw = rounded.toString()

    return if (raw.contains('.')) {
        val fractional = raw.substringAfter('.')
        when (fractional.length) {
            0 -> "$raw" + "00"
            1 -> "$raw" + "0"
            else -> raw
        }
    } else {
        "$raw.00"
    }
}