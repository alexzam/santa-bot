package az.santabot.storage.model

import az.santabot.model.DbUser
import az.santabot.storage.TxContext
import com.google.cloud.firestore.DocumentSnapshot

internal object GroupUsers : FirestoreCollection<GroupUser>("group_users") {
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

    fun remove(ctx: TxContext, uid: Int, gid: Int) =
        remove(ctx) { whereEqualTo("gid", gid).whereEqualTo("uid", uid) }

    fun getByGid(gid: Int) = find { whereEqualTo("gid", gid) }

    fun get(gid: Int, uid: Int, ctx: TxContext? = null) =
        findOne(ctx?.tx) { whereEqualTo("gid", gid).whereEqualTo("uid", uid) }

    fun updateTarget(ctx: TxContext, gid: Int, uid: Int, target: Int, targetName: String, targetUsername: String?) =
        update(
            ctx, querySetup = { whereEqualTo("gid", gid).whereEqualTo("uid", uid) }, mapOf(
                "target" to target,
                "target_name" to targetName,
                "target_username" to targetUsername
            )
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
