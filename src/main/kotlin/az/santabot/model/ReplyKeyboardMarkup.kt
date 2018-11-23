package az.santabot.model

import com.fasterxml.jackson.annotation.JsonProperty

class ReplyKeyboardMarkup(
    val keyboard: List<List<KeyboardButton>>,
    @JsonProperty("resize_keyboard") val resizeKeyboard: Boolean = false,
    @JsonProperty("one_time_keyboard") val onetimeKeyboard: Boolean = false
) : ReplyMarkup

class KeyboardButton(
    val text: String
)
