package az.santabot

import az.santabot.model.Update
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.jackson.jackson
import io.ktor.request.receive
import io.ktor.request.receiveText
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.random.nextUBytes

@ExperimentalUnsignedTypes
fun main(args: Array<String>) {

    // DI for tiny app
    val incomingToken = Random.nextUBytes(10).map { it.toString(16) }.fold("") { acc, s -> acc + s }
    val dbService = DbService()
    val santaService = SantaService()
    val telegramService = TelegramService(incomingToken, dbService)

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
                    println("IN: " + call.receiveText())
                    update = call.receive<Update>()
                    telegramService.onReceiveUpdate(update)
                    call.respondText("")
                } catch (e: Exception) {
                    println(e)
                    e.printStackTrace()
                }
            }
        }
    }.start(wait = true)
}