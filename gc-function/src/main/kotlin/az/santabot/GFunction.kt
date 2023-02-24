package az.santabot

import az.santabot.model.tg.Update
import az.santabot.storage.PostgresDbService
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.cloud.functions.HttpFunction
import com.google.cloud.functions.HttpRequest
import com.google.cloud.functions.HttpResponse
import kotlinx.coroutines.runBlocking

@Suppress("unused")
class GFunction : HttpFunction {
    private val host = System.getenv("HOST")
    private val inToken = System.getenv("IN_TOKEN")
    private val url = "https://$host/tg/$inToken"

    private val dbService = PostgresDbService()
    private val santaService = SantaService(dbService)
    private val telegramService = TelegramService(santaService, url)
        .also { santaService.telegramService = it }
    private val objectMapper = ObjectMapper()

    override fun service(request: HttpRequest, response: HttpResponse) {
        when (request.path) {
            "/tg/$inToken" -> {
                val update = request.reader.use { objectMapper.readValue(it, Update::class.java) }
                val resp = telegramService.onReceiveUpdate(update)
                response.writer.write(objectMapper.writeValueAsString(resp))
            }

            "/setup" -> {
                runBlocking { telegramService.setupEndpoint() }
            }

            else -> {
                response.setStatusCode(404)
            }
        }
    }
}