package az.santabot.model

import com.fasterxml.jackson.annotation.JsonProperty

interface InputMessageContent

enum class ParseMode { Markdown, HTML }

class InputTextMessageContent(
    @JsonProperty("message_text") val mesageText: String,
    @JsonProperty("parse_mode") val parseMode: ParseMode? = null
) : InputMessageContent