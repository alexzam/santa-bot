package az.santabot

import awaitString
import az.santabot.model.SetWebhookRequest
import az.santabot.model.Update
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.kittinunf.fuel.httpPost

class TelegramService(private val incomingToken: String) {
    private val token = System.getenv("TG_TOKEN")
    private val ownHost = System.getenv("OWN_HOST")
    private val mapper = ObjectMapper()

    suspend fun setupEndpoint(): String {
        val req = SetWebhookRequest("http://$ownHost/tg/$incomingToken")

        return methodUrl("setWebhook").httpPost()
            .jsonBody(mapper.writeValueAsString(req))
            .awaitString()
    }

    fun onReceiveUpdate(update: Update) {
        println(
            """Update received
            |   Id:         ${update.updateId}
            |   Inline id:  ${update.inlineQuery?.id}
            |   From:       ${update.inlineQuery?.from?.firstName} ${update.inlineQuery?.from?.lastName}
            |   From login: ${update.inlineQuery?.from?.username}
            |   Query:      ${update.inlineQuery?.query}
        """.trimMargin()
        )
    }

    fun sendToUser() {}

    private fun methodUrl(method: String) = "https://api.telegram.org/bot$token/$method"
}