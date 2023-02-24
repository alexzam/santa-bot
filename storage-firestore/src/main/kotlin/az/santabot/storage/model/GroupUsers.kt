package az.santabot.storage.model

import az.santabot.model.DbUser
import com.google.cloud.firestore.DocumentSnapshot

object GroupUsers : FirestoreCollection<GroupUser>("group_users") {
    override fun DocumentSnapshot.toModel(): GroupUser =
        GroupUser(
            gid = eGetInt("gid"),
            uid = eGetInt("uid"),
            username = eGetString("username"),
            display = eGetString("display")
        )

    override fun GroupUser.toFirebase(): Map<String, Any?> = mapOf(
        "gid" to gid,
        "uid" to uid,
        "username" to username,
        "display" to display
    )
}

class GroupUser(
    val gid: Int,
    val uid: Int,
    val username: String?,
    val display: String
) {
    fun toDbUser(): DbUser =
        DbUser(display, username)
}
