package az.santabot

import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun main(args: Array<String>) {
    val dbService = DbService()

    embeddedServer(Netty) {
        routing {
            get("/") {
                call.respondText(dbService.getUrlInfo(), ContentType.Text.Html)
            }
        }
    }.start(wait = true)
}