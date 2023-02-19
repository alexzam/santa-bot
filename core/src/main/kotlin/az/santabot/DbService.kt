package az.santabot

import az.santabot.model.User

interface DbService {
    fun getUrlInfo(): String
    fun getGroups(user: User): List<Group>
    fun findChatState(id: Int): SantaService.ChatState
    fun createGroupInChat(chatId: Int, name: String, user: User): Int
    fun addToGroup(gid: Int, user: User): Boolean
    fun removeFromGroup(gid: Int, user: User): Boolean
    fun getGroup(gid: Int): Group?
    fun setChatState(chatId: Int, state: SantaService.ChatState): Int
    fun getGroupsForClose(user: User): List<Group>
    fun findAdminGroupByName(user: User, name: String): Group?
    fun closeGroup(gid: Int): Int
    fun getGroupMembers(gid: Int): List<String>
    fun saveShuffled(gid: Int, shuffled: Map<String, String>)
    fun findTarget(gid: Int, user: User): DbUser?
}