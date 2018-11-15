package az.santabot.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
class Message @JsonCreator constructor(
    @JsonProperty("message_id") val id: Int,
    @JsonProperty("chat") val chat: Chat,
    @JsonProperty("from") val from: User?,
    @JsonProperty("text") val text: String?
)
