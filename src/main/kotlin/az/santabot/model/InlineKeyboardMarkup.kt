package az.santabot.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

class InlineKeyboardMarkup(@JsonProperty("inline_keyboard") val keyboard: List<InlineKeyboardButton>) : ReplyMarkup

@JsonInclude(JsonInclude.Include.NON_NULL)
class InlineKeyboardButton(
    val text: String,
    val url: String? = null,
    @JsonProperty("callback_data") val callbackData: String? = null,
    @JsonProperty("switch_inline_query") val switchInlineQuery: String? = null,
    @JsonProperty("switch_inline_query_current_chat") val switchInlineQueryCurrent: String? = null
)