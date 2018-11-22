package az.santabot

import awaitString
import az.santabot.model.Request
import az.santabot.model.SetWebhookRequest
import az.santabot.model.Update
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.kittinunf.fuel.httpPost

class TelegramService(
    private val incomingToken: String,
    private val santaService: SantaService
) {
    private val token = System.getenv("TG_TOKEN")
    private val ownHost = System.getenv("OWN_HOST")
    private val mapper = ObjectMapper()

    suspend fun setupEndpoint(): String {
        val req = SetWebhookRequest("https://$ownHost/tg/$incomingToken")

        return sendRequest(req)
    }

    suspend fun onReceiveUpdate(update: Update): Request? {
        if (update.inlineQuery != null) {
            return santaService.processInlineRequest(update.inlineQuery)
        }
        if (update.message != null) {
            return santaService.processMessage(update.message)
        }
        if (update.callbackQuery != null) {
            return santaService.processCallbackQuery(update.callbackQuery)
        }
        return null
    }

    suspend fun sendRequest(request: Request): String {
        val body = mapper.writeValueAsString(request)
        println("REQ: $body")

        val req = methodUrl(request.method).httpPost()
            .jsonBody(body)

        val ret = req.awaitString()
        println("RESP: $ret")

        return ret
    }

    private fun methodUrl(method: String) = "https://api.telegram.org/bot$token/$method"
}