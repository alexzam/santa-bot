package az.santabot.storage

import az.santabot.model.ChatState
import az.santabot.model.DbChat
import az.santabot.model.DbUser
import az.santabot.model.Group
import az.santabot.model.tg.User
import az.santabot.storage.model.*
import com.google.auth.oauth2.AccessToken
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.FirestoreOptions
import java.util.*

class FirestoreDbService(
    private val projectId: String,
    private val accessToken: String,
    expiration: Date
) : DbService {

    private val firestoreOptions: FirestoreOptions = FirestoreOptions.getDefaultInstance().toBuilder()
        .setProjectId(projectId)
        .setCredentials(GoogleCredentials.create(AccessToken(accessToken, expiration)))
        .build()

    private val db: Firestore = firestoreOptions.getService()

    override fun getUrlInfo(): String = "Project: $projectId, token ${accessToken.take(4)}…${accessToken.takeLast(4)}"

    override fun getGroups(user: User): List<Group> =
        Groups.find { whereArrayContains("users", user.id) }.map { it.toModel() }

    override fun findChatState(id: Int): ChatState {
        val existingChat = Chats.findById(id.toString())

        if (existingChat != null) return existingChat.state
        Chats.save(DbChat(id, ChatState.Idle))

        return ChatState.Idle
    }

    override fun createGroupInChat(chatId: Int, name: String, user: User): Int {
        Chats.save(DbChat(chatId, ChatState.Idle))

        //TODO Is "author" needed here? It was set as user.id.toString()
        val gid = Groups.save(DbGroup(-1, name, user.display, 1, false, listOf(user.id))).toInt()

        GroupUsers.save(
            GroupUser(
                gid = gid,
                uid = user.id,
                username = user.username,
                display = user.display
            )
        )

        return gid
    }

    override fun addToGroup(gid: Int, user: User): Boolean {
        return db.runTransaction { tx ->
            val group = Groups.findOne(tx) { whereEqualTo("gid", gid) } ?: throw Exception("Group not found")

            if (user.id in group.uids) return@runTransaction false

            Groups.save(group.withUids(group.uids + user.id), tx)

            GroupUsers.save(
                GroupUser(
                    gid = gid,
                    uid = user.id,
                    username = user.username,
                    display = user.display
                ), tx
            )

            true
        }.get()
    }

    override fun removeFromGroup(gid: Int, user: User): Boolean {
        return db.runTransaction { tx ->
            val group = Groups.findOne(tx) { whereEqualTo("gid", gid) } ?: throw Exception("Group not found")

            if (user.id !in group.uids) return@runTransaction false

            Groups.save(group.withUids(group.uids - user.id), tx)

            GroupUsers.remove(tx) { whereEqualTo("gid", gid).whereEqualTo("uid", user.id) }

            true
        }.get()
    }

    override fun getGroup(gid: Int): Group? =
        Groups.findById(gid.toString())?.toModel()

    override fun setChatState(chatId: Int, state: ChatState) {
        db.runTransaction { tx ->
            val chat = Chats.findById(chatId.toString(), tx)?.withState(state)
                ?: DbChat(chatId, state)

            Chats.save(chat, tx)
        }.get()
    }

    override fun getGroupsForClose(user: User): List<Group> =
        Groups.find { whereEqualTo("author", user.id).whereEqualTo("closed", false) }
            .map { it.toModel() }

    override fun findAdminGroupByName(user: User, name: String): Group? =
        Groups.findOne {
            whereEqualTo("author", user.id)
                .whereEqualTo("closed", false)
                .whereEqualTo("name", name)
        }?.toModel()

    override fun closeGroup(gid: Int) {
        db.runTransaction { tx ->
            Groups.findById(gid.toString(), tx)
                ?.withClosed(true)
                ?.also { Groups.save(it, tx) }
        }.get()
    }

    override fun getGroupMembers(gid: Int): List<String> =
        GroupUsers.find { whereEqualTo("gid", gid) }.map { it.uid.toString() }

    override fun saveShuffled(gid: Int, shuffled: Map<String, String>) {
        shuffled.forEach { (uid, target) ->
            val targetU = GroupUsers.findOne { whereEqualTo("gid", gid).whereEqualTo("uid", target) }
                ?: throw RuntimeException("User not found after shuffle")

            GroupUsers.update(
                null, querySetup = { whereEqualTo("gid", gid).whereEqualTo("uid", uid) }, mapOf(
                    "target" to target,
                    "target_name" to targetU.display,
                    "target_username" to targetU.username
                )
            )
        }
    }

    override fun findTarget(gid: Int, user: User): DbUser? =
        GroupUsers.findOne { whereEqualTo("gid", gid).whereEqualTo("uid", user.id) }
            ?.toDbUser()
}