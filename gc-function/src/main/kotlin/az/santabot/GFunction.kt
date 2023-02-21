package az.santabot

import az.santabot.storage.PostgresDbService
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.cloud.functions.HttpFunction
import com.google.cloud.functions.HttpRequest
import com.google.cloud.functions.HttpResponse

@Suppress("unused")
class GFunction : HttpFunction {
    private val url = System.getenv("FUN_URL")

    private val dbService = PostgresDbService()
    private val santaService = SantaService(dbService)
    private val telegramService = TelegramService(santaService, url)
        .also { santaService.telegramService = it }
    private val objectMapper = ObjectMapper()

    override fun service(request: HttpRequest, response: HttpResponse) {
        if (request.path == "/tg/incomingToken") {
//            val update = request.reader.use { objectMapper.readValue(it, Update::class.java) }
//            val resp = telegramService.onReceiveUpdate(update)
//            response.writer.write(objectMapper.writeValueAsString(resp))
            response.writer.write("inc!")
        } else if (request.path == "/setup") {
//            runBlocking { telegramService.setupEndpoint() }
            response.writer.write("OK")
        } else {
            response.writer.write("Called with path ${request.path}")
        }
    }
}