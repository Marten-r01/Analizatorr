package analizator

import analizator.dto.AnalyzeRequestDto
import analizator.dto.ErrorResponseDto
import analizator.dto.HealthResponseDto
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json

fun main() {
    embeddedServer(
        factory = Netty,
        port = 8080,
        host = "0.0.0.0",
        module = Application::module
    ).start(wait = true)
}

fun Application.module() {
    val analysisService = SequenceAnalysisService(
        parser = FastaParser(FastaValidator()),
        gcAnalyzer = GcAnalyzer(),
        orfFinder = OrfFinder(),
        dnaTranslator = DnaTranslator()
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
            post("/analyze") {
                val request = call.receive<AnalyzeRequestDto>()
                val report = analysisService.analyze(request.fastaContent.lineSequence().toList())
                call.respond(report.toDto())
            }
        }
    }
}
