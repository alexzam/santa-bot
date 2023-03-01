package az.santabot.storage

import az.santabot.model.ChatState
import az.santabot.model.DbChat
import az.santabot.model.DbUser
import az.santabot.model.Group
import az.santabot.model.tg.User
import az.santabot.storage.model.*
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.FirestoreOptions

class FirestoreDbService(
    private val projectId: String
) : DbService {

    private val firestoreOptions: FirestoreOptions = FirestoreOptions.getDefaultInstance().toBuilder()
        .setProjectId(projectId)
        .setCredentials(GoogleCredentials.getApplicationDefault())
        .build()

    private val db: Firestore = firestoreOptions.getService()
        .also { db ->
            listOf(
                Chats,
                Groups,
                GroupUsers
            ).forEach { it.db = db }
        }

    override fun getUrlInfo(): String = "Project: $projectId, creds ${firestoreOptions.credentials.toString()}"

    override fun getGroups(user: User): List<Group> =
        Groups.getAll(user.id)

    override fun findChatState(id: Int): ChatState {
        val existingChat = Chats.findById(id.toString())

        if (existingChat != null) return existingChat.state
        Chats.save(DbChat(id, ChatState.Idle))

        return ChatState.Idle
    }

    override fun createGroupInChat(chatId: Int, name: String, user: User): Int {
        return db.tx { ctx ->
            Chats.save(DbChat(chatId, ChatState.Idle), ctx)

            val gid = Groups.save(
                DbGroup(-1, name, user.display, 1, false, listOf(user.id), user.id),
                ctx
            ).toInt()

            GroupUsers.save(
                GroupUser(
                    gid = gid,
                    uid = user.id,
                    username = user.username,
                    display = user.display
                ),
                ctx
            )

            gid
        }.get()
    }

    override fun addToGroup(gid: Int, user: User): Boolean {
        return db.tx { ctx ->
            val group = Groups.findById(gid.toString(), ctx.tx) ?: throw Exception("Group not found: $gid")

            if (user.id in group.uids) return@tx false

            Groups.save(group.withUids(group.uids + user.id), ctx)

            GroupUsers.save(
                GroupUser(
                    gid = gid,
                    uid = user.id,
                    username = user.username,
                    display = user.display
                ),
                ctx
            )

            true
        }.get()
    }

    override fun removeFromGroup(gid: Int, user: User): Boolean {
        return db.tx { ctx ->
            val group = Groups.findById(gid.toString(), ctx.tx) ?: throw Exception("Group not found")

            if (user.id !in group.uids) return@tx false

            Groups.save(group.withUids(group.uids - user.id), ctx)

            GroupUsers.remove(ctx, user.id, gid)

            true
        }.get()
    }

    override fun getGroup(gid: Int): Group? =
        Groups.findById(gid.toString())?.toModel()

    override fun setChatState(chatId: Int, state: ChatState) {
        db.tx { ctx ->
            val chat = Chats.findById(chatId.toString(), ctx.tx)?.withState(state)
                ?: DbChat(chatId, state)

            Chats.save(chat, ctx)
        }.get()
    }

    override fun getGroupsForClose(user: User): List<Group> =
        Groups.getNotClosed(user.id)

    override fun findAdminGroupByName(user: User, name: String): Group? =
        Groups.getNotClosed(user.id, name)

    override fun closeGroup(gid: Int) {
        db.tx { ctx ->
            Groups.findById(gid.toString(), ctx.tx)
                ?.withClosed(true)
                ?.also { Groups.save(it, ctx) }
        }.get()
    }

    override fun getGroupMembers(gid: Int): List<String> =
        GroupUsers.getByGid(gid).map { it.uid.toString() }

    override fun saveShuffled(gid: Int, shuffled: Map<String, String>): Unit = db.tx { ctx ->
        shuffled.forEach { (uid, target) ->
            val targetId = target.toInt()
            val targetU = GroupUsers.get(gid, targetId, ctx)
                ?: throw RuntimeException("User not found after shuffle")

            GroupUsers.updateTarget(ctx, gid, uid.toInt(), targetId, targetU.display, targetU.username)
        }
    }.get()

    override fun findTarget(gid: Int, user: User): DbUser? =
        GroupUsers.get(gid, user.id)?.toDbUser()
}