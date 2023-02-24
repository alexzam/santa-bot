package az.santabot.model

class DbChat(
    val id: Int,
    val state: ChatState
) {
    fun withState(state: ChatState) =
        DbChat(
            id = id,
            state = state
        )
}