package az.santabot.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
class CallbackQuery
@JsonCreator constructor(
    @JsonProperty("id") val id: String,
    @JsonProperty("from") val from: User,
    @JsonProperty("data") val data: String?,
    @JsonProperty("inline_message_id") val inlineMessageId: String?
)
