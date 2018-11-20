package az.santabot.model

import com.fasterxml.jackson.annotation.JsonProperty

class AnswerCallbackQueryRequest(
    @JsonProperty("callback_query_id") val callbackQueryId: String,
    val text: String,
    @JsonProperty("show_alert") val showAlert: Boolean = false
) : Request("answerCallbackQuery")