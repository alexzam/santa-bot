package az.santabot.model

import az.santabot.util.Either
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
class SendMessageRequest(
    @JsonProperty("chat_id") val chatId: Either<Int, String>,
    val text: String,
    @JsonProperty("reply_markup") val replyMarkup: ReplyMarkup? = null
) : Request("sendMessage")