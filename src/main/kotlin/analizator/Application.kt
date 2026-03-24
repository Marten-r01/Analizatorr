package analizator

import analizator.dto.AnalyzeRequestDto
import analizator.dto.ErrorResponseDto
import analizator.dto.HealthResponseDto
import analizator.dto.UploadConfigResponseDto
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.contentType
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import java.io.IOException

fun main() {
    embeddedServer(
        factory = Netty,
        port = 8080,
        host = "0.0.0.0",
        module = Application::module
    ).start(wait = true)
}

fun Application.module(repository: AnalysisRepository = createProductionRepository()) {
    val analysisService = SequenceAnalysisService(
        parser = FastaParser(FastaValidator()),
        gcAnalyzer = GcAnalyzer(),
        orfFinder = OrfFinder(),
        dnaTranslator = DnaTranslator(),
        repository = repository
    )

    val multipartFastaExtractor = MultipartFastaExtractor(
        maxFileSizeBytes = UploadConstraints.MAX_FASTA_SIZE_BYTES
    )

    install(ContentNegotiation) {
        json(
            Json {
                prettyPrint = true
                ignoreUnknownKeys = true
            }
        )
    }

    install(StatusPages) {
        exception<IllegalArgumentException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponseDto(message = cause.message ?: "Некорректный запрос")
            )
        }

        exception<IOException> { call, _ ->
            call.respond(
                HttpStatusCode.PayloadTooLarge,
                ErrorResponseDto(
                    message = "Размер загружаемого FASTA-файла превышает ${UploadConstraints.MAX_FASTA_SIZE_MB} МБ"
                )
            )
        }

        exception<Throwable> { call, cause ->
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponseDto(message = cause.message ?: "Внутренняя ошибка сервера")
            )
        }
    }

    routing {
        get("/health") {
            call.respond(HealthResponseDto(status = "UP"))
        }

        route("/api/v1") {
            get("/upload-config") {
                call.respond(
                    UploadConfigResponseDto(
                        maxFileSizeBytes = UploadConstraints.MAX_FASTA_SIZE_BYTES,
                        maxFileSizeMb = UploadConstraints.MAX_FASTA_SIZE_MB,
                        fileFieldName = UploadConstraints.FILE_FIELD_NAME,
                        acceptedRequestContentType = ContentType.MultiPart.FormData.toString()
                    )
                )
            }

            post("/analyze") {
                val request = call.receive<AnalyzeRequestDto>()
                val report = analysisService.analyzeAndSave(
                    lines = request.fastaContent.lineSequence().toList(),
                    originalFileName = request.originalFileName
                )
                call.respond(report.toDto())
            }

            post("/analyze-upload") {
                require(call.request.contentType().match(ContentType.MultiPart.FormData)) {
                    "Ожидается Content-Type: multipart/form-data"
                }

                val uploadedFasta = multipartFastaExtractor.extract(call)
                val report = analysisService.analyzeAndSave(
                    lines = uploadedFasta.content.lineSequence().toList(),
                    originalFileName = uploadedFasta.originalFileName
                )

                call.respond(report.toDto())
            }

            get("/analysis/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: throw IllegalArgumentException("Некорректный id анализа")

                val report = analysisService.getByExperimentId(id)
                if (report == null) {
                    call.respond(
                        HttpStatusCode.NotFound,
                        ErrorResponseDto(message = "Анализ не найден")
                    )
                } else {
                    call.respond(report.toDto())
                }
            }
        }
    }
}

private fun createProductionRepository(): AnalysisRepository {
    val dbUrl = System.getenv("DB_URL") ?: "jdbc:postgresql://localhost:5432/analizator"
    val dbUser = System.getenv("DB_USER") ?: "postgres"
    val dbPassword = System.getenv("DB_PASSWORD") ?: "postgres"
    val dbDriver = System.getenv("DB_DRIVER") ?: "org.postgresql.Driver"

    DatabaseFactory.connect(
        url = dbUrl,
        driver = dbDriver,
        user = dbUser,
        password = dbPassword
    )
    DatabaseFactory.createSchema()

    return ExposedAnalysisRepository()
}
