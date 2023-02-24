package az.santabot.storage.model

import az.santabot.model.ChatState
import az.santabot.model.DbChat
import com.google.cloud.firestore.DocumentSnapshot

object Chats : FirestoreCollection<DbChat>("chats") {
    override fun DocumentSnapshot.toModel(): DbChat =
        DbChat(
            id = id.toInt(),
            state = ChatState.find(eGetInt("state")) ?: throw Exception("Unknown chat state in ${reference.path}")
        )

    override fun DbChat.toFirebase(): Map<String, Any?> = mapOf(
        // ID is natural
        "state" to state.id
    )

    override fun DbChat.naturalId(): String = id.toString()
}