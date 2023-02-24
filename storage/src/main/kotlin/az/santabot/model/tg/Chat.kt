package az.santabot.model.tg

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
class Chat
@JsonCreator constructor(
    @JsonProperty("id") val id: Int,

    )