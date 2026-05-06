package analizator.frontend

import org.w3c.files.File

data class SelectedFile(
    val file: File,
    val name: String,
    val sizeBytes: Int
)
