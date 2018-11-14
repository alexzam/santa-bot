package az.santabot

import awaitString
import az.santabot.model.SetWebhookRequest
import az.santabot.model.Update
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.kittinunf.fuel.httpPost
import org.slf4j.LoggerFactory

class TelegramService(private val incomingToken: String) {
    private val logger = LoggerFactory.getLogger(TelegramService::class.java)
    private val token = System.getenv("TG_TOKEN")
    private val ownHost = System.getenv("OWN_HOST")
    private val mapper = ObjectMapper()

    suspend fun setupEndpoint(): String {
        val req = SetWebhookRequest("http://$ownHost/tg/$incomingToken")

        val request = methodUrl("setWebhook").httpPost()
            .jsonBody(mapper.writeValueAsString(req))

        logger.info("Request: " + request.cUrlString())
        return request.awaitString()
    }

    fun onReceiveUpdate(update: Update) {
        logger.info(
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