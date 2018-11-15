package az.santabot.model

import az.santabot.util.Either
import com.fasterxml.jackson.annotation.JsonProperty

class SendMessageRequest(
    @JsonProperty("chat_id") val chatId: Either<Int, String>,
    val text: String
) : Request("sendMessage")