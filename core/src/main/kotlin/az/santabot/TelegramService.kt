package az.santabot

import az.santabot.model.Request
import az.santabot.model.SetWebhookRequest
import az.santabot.model.Update
import com.github.kittinunf.fuel.coroutines.awaitStringResult
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.fuel.jackson.objectBody
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.getOrElse
import com.github.kittinunf.result.success

class TelegramService(
    private val incomingToken: String,
    private val santaService: SantaService
) {
    private val token = System.getenv("TG_TOKEN")
    private val ownHost = System.getenv("OWN_HOST")

    suspend fun setupEndpoint(): String {
        val req = SetWebhookRequest("https://$ownHost/tg/$incomingToken")

        return sendRequest(req)
    }

    fun onReceiveUpdate(update: Update): Request? {
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
        val req = methodUrl(request.method).httpPost()
            .objectBody(request)

        val ret = req.awaitStringResult()

        ret.failure { println("ERR: ${it.response.data.toString(Charsets.UTF_8)}") }
        ret.success { println("RESP: $ret") }

        return ret.getOrElse { "<no result>" }
    }

    private fun methodUrl(method: String) = "https://api.telegram.org/bot$token/$method"
}