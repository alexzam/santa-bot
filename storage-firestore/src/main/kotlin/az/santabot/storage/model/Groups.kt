package az.santabot.storage.model

import com.google.cloud.firestore.DocumentSnapshot

internal object Groups : FirestoreCollection<DbGroup>("groups") {
    override fun DocumentSnapshot.toModel(): DbGroup =
        DbGroup(
            id = id.toInt(),
            name = eGetString("name"),
            authorName = eGetString("authorName"),
            membersNum = eGetInt("membersNum"),
            closed = eGetBoolean("closed"),
            uids = eGetArray<Long>("uids").map { it.toInt() },
            author = eGetInt("author")
        )

    override fun DbGroup.toFirebase(): Map<String, Any?> = mapOf(
        "name" to name,
        "authorName" to authorName,
        "membersNum" to membersNum,
        "closed" to closed,
        "uids" to uids,
        "author" to author
    )

    override fun DbGroup.naturalId(): String = id.toString()

    fun getAll(uid: Int) = find { whereArrayContains("uids", uid) }.map { it.toModel() }

    fun getNotClosed(uid: Int) = find { whereEqualTo("author", uid).whereEqualTo("closed", false) }
        .map { it.toModel() }

    fun getNotClosed(uid: Int, name: String) = findOne {
        whereEqualTo("author", uid)
            .whereEqualTo("closed", false)
            .whereEqualTo("name", name)
    }?.toModel()
}