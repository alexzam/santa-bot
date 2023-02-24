package az.santabot.model.tg

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
class Update
@JsonCreator constructor(
    @JsonProperty("update_id") val updateId: Int,
    @JsonProperty("inline_query") val inlineQuery: InlineQuery?,
    @JsonProperty("message") val message: Message?,
    @JsonProperty("callback_query") val callbackQuery: CallbackQuery?
)
