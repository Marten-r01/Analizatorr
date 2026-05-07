package analizator

import io.ktor.http.content.PartData
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receiveMultipart
import io.ktor.utils.io.readAvailable
import java.io.IOException

class MultipartFastaExtractor(
    private val maxFileSizeBytes: Int
) {
    suspend fun extract(call: ApplicationCall): UploadedFasta {
        val multipartData = call.receiveMultipart(formFieldLimit = maxFileSizeBytes.toLong())
        var uploadedFasta: UploadedFasta? = null

        while (true) {
            val part = multipartData.readPart() ?: break

            when (part) {
                is PartData.FileItem -> {
                    try {
                        if (part.name == UploadConstraints.FILE_FIELD_NAME && uploadedFasta == null) {
                            uploadedFasta = UploadedFasta(
                                originalFileName = part.originalFileName?.takeIf { it.isNotBlank() },
                                record = readFirstFastaRecord(part)
                            )
                        }
                    } finally {
                        part.dispose()
                    }
                }

                else -> part.dispose()
            }
        }

        return requireNotNull(uploadedFasta) {
            "В multipart-запросе отсутствует файл в поле '${UploadConstraints.FILE_FIELD_NAME}'"
        }
    }

    private suspend fun readFirstFastaRecord(part: PartData.FileItem): FastaRecord {
        val channel = part.provider()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        val header = StringBuilder()
        val sequence = StringBuilder()
        var bytesReadTotal = 0L
        var firstHeaderSeen = false
        var readingHeader = false
        var headerCompleted = false
        var atLineStart = true

        while (true) {
            val bytesRead = channel.readAvailable(buffer, 0, buffer.size)
            if (bytesRead == -1) break

            bytesReadTotal += bytesRead
            if (bytesReadTotal > maxFileSizeBytes) {
                throw IOException(
                    "Размер загружаемого FASTA-файла превышает ${UploadConstraints.MAX_FASTA_SIZE_MB} МБ"
                )
            }

            for (index in 0 until bytesRead) {
                val char = buffer[index].toInt().toChar()

                if (char == '\r' || char == '\n') {
                    if (readingHeader) {
                        readingHeader = false
                        headerCompleted = true
                    }
                    atLineStart = true
                    continue
                }

                if (atLineStart && char.isWhitespace()) {
                    continue
                }

                if (atLineStart && char == '>') {
                    if (firstHeaderSeen && headerCompleted) {
                        return buildRecord(header, sequence)
                    }

                    firstHeaderSeen = true
                    readingHeader = true
                    atLineStart = false
                    continue
                }

                if (!firstHeaderSeen) {
                    throw IllegalArgumentException("FASTA-заголовок должен начинаться с символа '>'")
                }

                atLineStart = false

                if (readingHeader) {
                    header.append(char)
                } else {
                    val normalized = char.uppercaseChar()
                    if (normalized != 'N' && !normalized.isWhitespace()) {
                        sequence.append(normalized)
                    }
                }
            }
        }

        require(bytesReadTotal > 0) { "Загруженный файл пустой" }
        return buildRecord(header, sequence)
    }

    private fun buildRecord(header: StringBuilder, sequence: StringBuilder): FastaRecord {
        val record = FastaRecord(
            header = header.toString().trim(),
            sequence = sequence.toString()
        )

        FastaValidator().validateHeader(record.header)
        FastaValidator().validateSequence(record.sequence)

        return record
    }
}
