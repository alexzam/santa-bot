package az.santabot.model

import az.santabot.util.Either
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@Suppress("unused")
@JsonInclude(JsonInclude.Include.NON_NULL)
class SendMessageRequest(
    chatId: Any,
    val text: String,
    @JsonProperty("reply_markup") val replyMarkup: ReplyMarkup? = null
) : Request("sendMessage") {
    @JsonProperty("chat_id")
    val chatIdInt: Either<Int, String> = Either(chatId)
}