package az.santabot.storage.model

import com.google.cloud.firestore.DocumentSnapshot

object Groups : FirestoreCollection<DbGroup>("groups") {
    override fun DocumentSnapshot.toModel(): DbGroup =
        DbGroup(
            id = id.toInt(),
            name = eGetString("name"),
            authorName = eGetString("authorName"),
            membersNum = eGetInt("membersNum"),
            closed = eGetBoolean("closed"),
            uids = eGetArray("uids")
        )

    override fun DbGroup.toFirebase(): Map<String, Any?> = mapOf(
        "name" to name,
        "authorName" to authorName,
        "membersNum" to membersNum,
        "closed" to closed,
        "uids" to uids
    )
}