package az.santabot.storage

import az.santabot.model.ChatState
import az.santabot.model.DbUser
import az.santabot.model.Group
import az.santabot.model.tg.User

interface DbService {
    fun getUrlInfo(): String
    fun getGroups(user: User): List<Group>
    fun findChatState(id: Int): ChatState
    fun createGroupInChat(chatId: Int, name: String, user: User): Int

    /**
     * @return `false` if user is already in the group
     */
    fun addToGroup(gid: Int, user: User): Boolean

    /**
     * @return `false` if user is already not in group.
     */
    fun removeFromGroup(gid: Int, user: User): Boolean
    fun getGroup(gid: Int): Group?
    fun setChatState(chatId: Int, state: ChatState)
    fun getGroupsForClose(user: User): List<Group>
    fun findAdminGroupByName(user: User, name: String): Group?
    fun closeGroup(gid: Int)
    fun getGroupMembers(gid: Int): List<String>
    fun saveShuffled(gid: Int, shuffled: Map<String, String>)
    fun findTarget(gid: Int, user: User): DbUser?
}