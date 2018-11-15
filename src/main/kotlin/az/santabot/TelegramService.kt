package az.santabot

import awaitString
import az.santabot.model.InlineQueryResponse
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

        val request = methodUrl("setWebhook").httpPost()
            .jsonBody(mapper.writeValueAsString(req))

        return request.awaitString()
    }

    fun onReceiveUpdate(update: Update): InlineQueryResponse? {
        if (update.inlineQuery != null) {
            // Get groups list
            return santaService.processInlineRequest(update.inlineQuery)
        }
        return null
    }

    fun sendToUser() {}

    private fun methodUrl(method: String) = "https://api.telegram.org/bot$token/$method"
}