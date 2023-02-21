package az.santabot.app

import az.santabot.SantaService
import az.santabot.TelegramService
import az.santabot.model.Update
import az.santabot.storage.PostgresDbService
import az.santabot.util.genToken
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.runBlocking

fun main() {
    embeddedServer(Netty, System.getenv("PORT")?.toIntOrNull() ?: 80, module = Application::santaBotModule)
        .start(wait = true)
}

@OptIn(ExperimentalUnsignedTypes::class)
fun Application.santaBotModule() {
    // DI for tiny app
    val immediateResponseMode = false

    val dbService = PostgresDbService()

    val santaService = SantaService(dbService)
    val ownHost = System.getenv("OWN_HOST")

    val baseUrl = "https://$ownHost/tg/"
    val incomingToken = genToken()
    val telegramService = TelegramService(santaService, baseUrl + incomingToken)

    santaService.telegramService = telegramService

    runBlocking { println("Telegram endpoint setup ($incomingToken): " + telegramService.setupEndpoint()) }

    install(ContentNegotiation) {
        jackson()
    }

    routing {
        get("/") {
            call.respondText(dbService.getUrlInfo(), ContentType.Text.Html)
        }
        post("/tg/$incomingToken") {
            try {
                val update = call.receive<Update>()
                val response = telegramService.onReceiveUpdate(update)

                if (immediateResponseMode) {
                    call.respond(response ?: "")
                } else {
                    call.respond(HttpStatusCode.OK, "")
                    response?.also { telegramService.sendRequest(it) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}