package az.santabot.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

abstract class InlineQueryResult(val type: String, val id: String)

@JsonInclude(JsonInclude.Include.NON_NULL)
class InlineQueryResultArticle(
    id: String,
    val title: String,
    @JsonProperty("input_message_content") val inputMessageContent: InputMessageContent,
    @JsonProperty("reply_markup") val replyMarkup: InlineKeyboardMarkup? = null,
    @JsonProperty("hide_url") val hideUrl: Boolean? = null,
    val description: String? = null

) : InlineQueryResult("article", id)
