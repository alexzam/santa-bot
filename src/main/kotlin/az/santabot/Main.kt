package az.santabot

import az.santabot.model.Update
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.jackson.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.runBlocking
import kotlin.random.Random
import kotlin.random.nextUBytes

@ExperimentalUnsignedTypes
fun main(args: Array<String>) {

    val immediateResponseMode = false

    // DI for tiny app
    val incomingToken = Random.nextUBytes(10).map { it.toString(16) }.fold("") { acc, s -> acc + s }
    val dbService = DbService()
    val santaService = SantaService(dbService)
    val telegramService = TelegramService(incomingToken, santaService)
    santaService.telegramService = telegramService
    val jacksonObjectMapper = jacksonObjectMapper()

    runBlocking { println("Telegram endpoint setup ($incomingToken): " + telegramService.setupEndpoint()) }

    embeddedServer(Netty, System.getenv("PORT").toIntOrNull() ?: 80) {
        install(ContentNegotiation) {
            jackson {}
        }
        routing {
            get("/") {
                call.respondText(dbService.getUrlInfo(), ContentType.Text.Html)
            }
            post("/tg/$incomingToken") {
                val update: Update
                try {
                    val receiveText = call.receiveText()
                    println("IN: $receiveText")
                    update = jacksonObjectMapper.readValue(receiveText, Update::class.java)
                    val response = telegramService.onReceiveUpdate(update)
                    val responseStr = jacksonObjectMapper.writeValueAsString(response)

                    if (immediateResponseMode) {
                        println("OUT: $responseStr")
                        call.respond(response ?: "")
                    } else {
                        call.respond(HttpStatusCode.OK, "")
                        response?.also { telegramService.sendRequest(it) }
                    }
                } catch (e: Exception) {
                    println(e)
                    e.printStackTrace()
                }
            }
        }
    }.start(wait = true)
}