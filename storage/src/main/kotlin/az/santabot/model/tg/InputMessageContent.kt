package az.santabot.model.tg

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

interface InputMessageContent

@JsonInclude(JsonInclude.Include.NON_NULL)
class InputTextMessageContent(
    @JsonProperty("message_text") val mesageText: String,
    @JsonProperty("parse_mode") val parseMode: ParseMode? = null
) : InputMessageContent