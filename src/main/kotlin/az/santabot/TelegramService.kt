package az.santabot

import awaitString
import az.santabot.model.*
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.kittinunf.fuel.httpPost
import org.slf4j.LoggerFactory

class TelegramService(
    private val incomingToken: String,
    private val dbService: DbService
) {
    private val logger = LoggerFactory.getLogger(TelegramService::class.java)
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
            val groups = dbService.getGroups(update.inlineQuery.from.id)
            return InlineQueryResponse(
                inlineQueryId = update.inlineQuery.id,
                personal = true,
                results = groups.map {
                    InlineQueryResultArticle(
                        id = it.id.toString(),
                        title = it.name,
                        inputMessageContent = InputTextMessageContent("g" + it.id)
                    )
                },
                switchPmText = "New group"
            )
        }
        return null
    }

    fun sendToUser() {}

    private fun methodUrl(method: String) = "https://api.telegram.org/bot$token/$method"
}