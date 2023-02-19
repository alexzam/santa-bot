package az.santabot.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
class Chat
@JsonCreator constructor(
    @JsonProperty("id") val id: Int
)

enum class ChatState(val id: Int) {
    CreateGetName(0),
    Idle(1),
    CloseGetName(2);

    companion object {
        fun find(id: Int): ChatState? = values().find { it.id == id }
    }

    override fun toString(): String = id.toString()
}