package az.santabot.model

enum class ChatState(val id: Int) {
    CreateGetName(0),
    Idle(1),
    CloseGetName(2);

    companion object {
        fun find(id: Int): ChatState? = values().find { it.id == id }
    }

    override fun toString(): String = id.toString()
}