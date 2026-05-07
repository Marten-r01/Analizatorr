package analizator

data class UploadedFasta(
    val originalFileName: String?,
    val record: FastaRecord
)
