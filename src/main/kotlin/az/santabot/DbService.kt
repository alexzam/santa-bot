package az.santabot

import az.santabot.model.User
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.Statement


class DbService {
    private val dbUrl = System.getenv("JDBC_DATABASE_URL")

    fun getUrlInfo(): String = "JDU:${System.getenv("JDBC_DATABASE_URL")}, DU:${System.getenv("DATABASE_URL")}"

    fun getGroups(user: User): List<Group> {
        var query = "SELECT g.* FROM groups AS g JOIN user_groups AS ug ON ug.gid=g.id WHERE ug.uid = ?"
        val username = user.username
        if (username != null) query += " OR ug.uid = ?"

        return withConnection {
            val statement = prepareStatement(query)
            statement.setString(1, user.id.toString())
            if (username != null) statement.setString(2, username)

            val results = statement.executeQuery()

            val ret = mutableListOf<Group>()
            while (results.next()) {
                ret += Group(results)
            }
            statement.close()

            ret
        }
    }

    fun findChatState(id: Int): SantaService.ChatState {
        return withConnection {
            val statement = prepareStatement("SELECT state FROM chats WHERE id = ?")
            statement.setInt(1, id)
            val resultSet = statement.executeQuery()
            if (!resultSet.next()) {
                val stCreate = prepareStatement("INSERT INTO chats(id, state) VALUES (?, ?)")
                stCreate.setInt(1, id)
                stCreate.setInt(2, SantaService.ChatState.Idle.id)
                stCreate.executeUpdate()

                SantaService.ChatState.Idle
            } else {
                SantaService.ChatState.find(resultSet.getInt(1))!!
            }
        }
    }

    fun createGroupInChat(chatId: Int, name: String, user: User): Int {
        return withConnection {
            val chatStatement = prepareStatement("UPDATE chats SET state=? WHERE id=?")
            chatStatement.setInt(1, SantaService.ChatState.Idle.id)
            chatStatement.setInt(2, chatId)
            chatStatement.executeUpdate()

            val groupStatement =
                prepareStatement(
                    "INSERT INTO groups(name, author, author_name) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
                )
            groupStatement.setString(1, name)
            groupStatement.setString(2, user.id.toString())
            groupStatement.setString(3, user.display)
            groupStatement.executeUpdate()
            val keys = groupStatement.generatedKeys
            keys.next()
            val gid = keys.getInt(1)

            val memberStatement =
                prepareStatement("INSERT INTO user_groups(gid, uid, u_username, u_name) VALUES (?, ?, ?, ?)")
            memberStatement.setInt(1, gid)
            memberStatement.setString(2, user.id.toString())
            memberStatement.setString(3, user.username)
            memberStatement.setString(4, "${user.firstName} ${user.lastName ?: ""}".trim())
            memberStatement.executeUpdate()

            gid
        }
    }

    fun addToGroup(gid: Int, user: User): Boolean {
        return withConnection {
            try {
                val st = prepareStatement("INSERT INTO user_groups(gid, uid, u_name, u_username) VALUES (?, ?, ?, ?)")
                st.setInt(1, gid)
                st.setString(2, user.id.toString())
                st.setString(3, (user.firstName + " " + (user.lastName ?: "")).trim())
                st.setString(4, user.username)
                st.executeUpdate()

                val numSt = prepareStatement("UPDATE groups SET memberNum = memberNum + 1 WHERE id = ?")
                numSt.setInt(1, gid)
                numSt.executeUpdate()
                true
            } catch (ignored: Exception) {
                false
            }
        }
    }

    fun removeFromGroup(gid: Int, user: User): Boolean {
        return withConnection {
            try {
                val st = prepareStatement("DELETE FROM user_groups WHERE gid = ? AND (uid = ? OR uid = ?)")
                st.setInt(1, gid)
                st.setString(2, user.id.toString())
                st.setString(3, user.username)
                if (st.executeUpdate() < 1) return@withConnection false

                val numSt = prepareStatement("UPDATE groups SET memberNum = memberNum - 1 WHERE id = ?")
                numSt.setInt(1, gid)
                numSt.executeUpdate()
                true
            } catch (ignored: Exception) {
                false
            }
        }
    }

    fun getGroup(gid: Int): Group? {
        return withConnection {
            val st = prepareStatement("SELECT * FROM groups WHERE id = ?")
            st.setInt(1, gid)
            val results = st.executeQuery()

            if (results.next()) Group(results) else null
        }
    }

    fun setChatState(chatId: Int, state: SantaService.ChatState) = withConnection {
        val st = prepareStatement(
            "INSERT INTO chats (id, started, state) VALUES (?, now(), ?) " +
                    "ON CONFLICT(id) DO UPDATE SET started = now(), state = ?"
        )
        st.setInt(1, chatId)
        st.setInt(2, state.id)
        st.setInt(3, state.id)
        st.executeUpdate()
    }

    fun getGroupsForClose(user: User): List<Group> = withConnection {
        val st = prepareStatement("SELECT * FROM groups WHERE author = ? AND closed = false")
        st.setString(1, user.id.toString())
        val results = st.executeQuery()

        val ret = mutableListOf<Group>()
        while (results.next()) {
            ret += Group(results)
        }
        st.close()

        ret
    }

    fun findAdminGroupByName(user: User, name: String): Group? = withConnection {
        val st = prepareStatement("SELECT * FROM groups WHERE author = ? AND name = ? AND closed = false")
        st.setString(1, user.id.toString())
        st.setString(2, name)
        val results = st.executeQuery()

        if (results.next()) {
            Group(results)
        } else {
            null
        }
    }

    fun closeGroup(gid: Int) = withConnection {
        val st = prepareStatement("UPDATE groups SET closed = true WHERE id = ?")
        st.setInt(1, gid)
        st.executeUpdate()
    }

    fun getGroupMembers(gid: Int): List<String> = withConnection {
        val st = prepareStatement("SELECT uid FROM user_groups WHERE gid = ?")
        st.setInt(1, gid)
        val results = st.executeQuery()

        val ret = mutableListOf<String>()
        while (results.next()) {
            ret += results.getString(1)
        }

        ret
    }

    fun saveShuffled(gid: Int, shuffled: Map<String, String>) = withConnection {
        shuffled.forEach { uid, target ->
            val getSt = prepareStatement("SELECT u_name, u_username FROM user_groups WHERE gid = ? AND uid = ?")
            getSt.setInt(1, gid)
            getSt.setString(2, target)
            val result = getSt.executeQuery()
            if (!result.next()) throw RuntimeException("User not found after shuffle")

            val name = result.getString(1)
            val username = result.getString(2)

            val st =
                prepareStatement("UPDATE user_groups SET target = ?, target_name = ?, target_username = ? WHERE gid = ? AND uid = ?")
            st.setString(1, target)
            st.setString(2, name)
            st.setString(3, username)
            st.setInt(4, gid)
            st.setString(5, uid)

            st.executeUpdate()
        }
    }

    fun findTarget(gid: Int, user: User): DbUser? = withConnection {
        val st =
            prepareStatement("SELECT target, target_name, target_username FROM user_groups WHERE gid = ? AND (uid = ? OR uid = ?)")
        st.setInt(1, gid)
        st.setString(2, user.id.toString())
        st.setString(3, user.username)
        val result = st.executeQuery()

        if (result.next()) DbUser(result) else null
    }

    private fun <T> withConnection(action: Connection.() -> T): T {
        DriverManager.getConnection(dbUrl)!!.use {
            return it.action()
        }
    }
}

class Group(results: ResultSet) {
    val id = results.getInt("id")
    val name = results.getString("name")!!
    val authorName = results.getString("author_name")!!
    val membersNum = results.getInt("memberNum")
    val closed: Boolean = results.getBoolean("closed")
}

class DbUser(results: ResultSet) {
    private val name: String? = results.getString("target_name")
    private val username: String? = results.getString("target_username")

    override fun toString(): String =
        if (name != null && username != null) "$name (@$username)"
        else if (username != null) "@$username"
        else name ?: "Fully anonymous user"
}
