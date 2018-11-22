package az.santabot.model

import com.fasterxml.jackson.annotation.JsonProperty

class EditMessageTextRequest(
    @JsonProperty("inline_message_id") val inlineMessageId: String? = null,
    val text: String,
    @JsonProperty("parse_mode") val parseMode: ParseMode? = null,
    @JsonProperty("reply_markup") val replyMarkup: ReplyMarkup? = null
) : Request("editMessageText")