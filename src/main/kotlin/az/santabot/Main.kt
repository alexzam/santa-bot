package az.santabot

import az.santabot.model.Update
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.jackson.jackson
import io.ktor.request.receiveText
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.random.nextUBytes

@ExperimentalUnsignedTypes
fun main(args: Array<String>) {

    // DI for tiny app
    val incomingToken = Random.nextUBytes(10).map { it.toString(16) }.fold("") { acc, s -> acc + s }
    val dbService = DbService()
    val santaService = SantaService(dbService)
    val telegramService = TelegramService(incomingToken, santaService)

    GlobalScope.launch { println("Telegram endpoint setup ($incomingToken): " + telegramService.setupEndpoint()) }

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
                    update = jacksonObjectMapper().readValue(receiveText, Update::class.java)
                    val response = telegramService.onReceiveUpdate(update) ?: ""
                    println("OUT: " + jacksonObjectMapper().writeValueAsString(response))
                    call.respond(response)
                    delay(10)
                } catch (e: Exception) {
                    println(e)
                    e.printStackTrace()
                }
            }
        }
    }.start(wait = true)
}