package analizator

import io.ktor.http.content.PartData
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receiveMultipart
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.copyAndClose
import kotlin.io.path.createTempFile

class MultipartFastaExtractor(
    private val maxFileSizeBytes: Int
) {
    suspend fun extract(call: ApplicationCall): UploadedFasta {
        val multipartData = call.receiveMultipart(formFieldLimit = maxFileSizeBytes.toLong())
        var uploadedFasta: UploadedFasta? = null

        multipartData.forEachPart { part ->
            when (part) {
                is PartData.FileItem -> {
                    if (part.name == UploadConstraints.FILE_FIELD_NAME && uploadedFasta == null) {
                        val bytes = readPartBytes(part)

                        require(bytes.isNotEmpty()) { "Загруженный файл пустой" }
                        require(bytes.size <= maxFileSizeBytes) {
                            "Размер загружаемого FASTA-файла превышает ${UploadConstraints.MAX_FASTA_SIZE_MB} МБ"
                        }

                        uploadedFasta = UploadedFasta(
                            originalFileName = part.originalFileName?.takeIf { it.isNotBlank() },
                            content = bytes.toString(Charsets.UTF_8)
                        )
                    }
                }

                else -> Unit
            }

            part.dispose()
        }

        return requireNotNull(uploadedFasta) {
            "В multipart-запросе отсутствует файл в поле '${UploadConstraints.FILE_FIELD_NAME}'"
        }
    }

    private suspend fun readPartBytes(part: PartData.FileItem): ByteArray {
        val tempFile = createTempFile(prefix = "analizator-upload-", suffix = ".tmp").toFile()

        return try {
            part.provider().copyAndClose(tempFile.writeChannel())
            tempFile.readBytes()
        } finally {
            tempFile.delete()
        }
    }
}
