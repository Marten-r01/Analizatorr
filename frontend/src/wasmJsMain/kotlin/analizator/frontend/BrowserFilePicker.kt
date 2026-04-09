package analizator.frontend

import kotlinx.browser.document
import org.w3c.dom.HTMLInputElement
import org.w3c.files.File

fun pickSingleFastaFile(onSelected: (File?) -> Unit) {
    val input = document.createElement("input") as HTMLInputElement
    input.type = "file"
    input.accept = ".fasta,.fa,.fna,.txt"
    input.multiple = false
    input.onchange = {
        onSelected(input.files?.item(0))
        null
    }
    input.click()
}