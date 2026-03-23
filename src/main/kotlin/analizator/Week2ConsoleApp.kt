package analizator

import java.io.File
import kotlin.system.exitProcess

object Week2ConsoleApp {
    @JvmStatic
    fun main(args: Array<String>) {
        runCatching {
            require(args.size == 1) { "Укажи путь к FASTA-файлу: ./gradlew run --args=\"sample.fasta\"" }

            val file = File(args[0])
            require(file.exists()) { "Файл не найден: ${file.path}" }
            require(file.isFile) { "Указанный путь не является файлом: ${file.path}" }

            val service = SequenceAnalysisService(
                parser = FastaParser(FastaValidator()),
                gcAnalyzer = GcAnalyzer(),
                orfFinder = OrfFinder()
            )

            val report = service.analyze(file.readLines())
            formatReport(report)
        }.onSuccess {
            println(it)
        }.onFailure {
            System.err.println("Ошибка: ${it.message}")
            exitProcess(1)
        }
    }

    private fun formatReport(report: SequenceReport): String {
        val headerBlock = buildString {
            appendLine("Header: ${report.header}")
            appendLine("Sequence: ${report.sequence}")
            appendLine("Length: ${report.stats.length}")
            appendLine("A: ${report.stats.aCount}")
            appendLine("T: ${report.stats.tCount}")
            appendLine("G: ${report.stats.gCount}")
            appendLine("C: ${report.stats.cCount}")
            appendLine("GC-content: ${"%.2f".format(report.stats.gcPercent)}%")
            appendLine("ORFs found: ${report.orfs.size}")
        }

        val orfBlock = if (report.orfs.isEmpty()) {
            "No ORFs found"
        } else {
            report.orfs.mapIndexed { index, orf ->
                "${index + 1}) frame=${orf.frame} start=${orf.start} end=${orf.end} length=${orf.length} sequence=${orf.sequence}"
            }.joinToString(separator = "\n")
        }

        return "$headerBlock$orfBlock".trimEnd()
    }
}
