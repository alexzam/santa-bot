package az.santabot

import az.santabot.model.User
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.Statement


class DbService {
    private val dbUrl = System.getenv("JDBC_DATABASE_URL")

    fun getUrlInfo(): String = "JDU:${System.getenv("JDBC_DATABASE_URL")}, DU:${System.getenv("DATABASE_URL")}"

    fun getGroups(uid: Int): List<Group> {
        return withConnection {
            val statement =
                prepareStatement("SELECT g.* FROM groups AS g JOIN user_groups AS ug ON ug.gid=g.id WHERE ug.uid=?")
            statement.setInt(1, uid)
            val results = statement.executeQuery()

            val ret = mutableListOf<Group>()
            while (results.next()) {
                ret += Group(results)
            }
            statement.close()

            ret
        }
    }

    fun saveStartChat(id: Int) {
        withConnection {
            val statementIns = prepareStatement("INSERT INTO chats(id, started) VALUES (?, NOW()) ")
            statementIns.setInt(1, id)
            statementIns.executeUpdate()
        }
    }

    fun findChatState(id: Int): Int? {
        return withConnection {
            val statement = prepareStatement("SELECT state FROM chats WHERE id = ?")
            statement.setInt(1, id)
            val resultSet = statement.executeQuery()
            if (resultSet.next()) resultSet.getInt(1) else null
        }
    }

    fun createGroupInChat(chatId: Int, name: String, user: User): Int {
        return withConnection {
            val chatStatement = prepareStatement("UPDATE chats SET state=1 WHERE id=?")
            chatStatement.setInt(1, chatId)
            chatStatement.executeUpdate()

            val groupStatement =
                prepareStatement("INSERT INTO groups(name, author) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS)
            groupStatement.setString(1, name)
            groupStatement.setString(2, user.username!!)
            groupStatement.executeUpdate()
            val keys = groupStatement.generatedKeys
            keys.next()
            val gid = keys.getInt(1)

            val memberStatement = prepareStatement("INSERT INTO user_groups(gid, uid) VALUES (?, ?)")
            memberStatement.setInt(1, gid)
            memberStatement.setInt(2, user.id)
            memberStatement.executeUpdate()

            gid
        }
    }

    fun addToGroup(gid: Int, uid: Int): Boolean {
        return withConnection {
            try {
                val st = prepareStatement("INSERT INTO user_groups(gid, uid) VALUES (?, ?)")
                st.setInt(1, gid)
                st.setInt(2, uid)
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

    fun removeFromGroup(gid: Int, uid: Int): Boolean {
        return withConnection {
            try {
                val st = prepareStatement("DELETE FROM user_groups WHERE gid = ? AND uid = ?")
                st.setInt(1, gid)
                st.setInt(2, uid)
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

    fun startCloseChat(chatId: Int) = withConnection {
        val st = prepareStatement(
            "INSERT INTO chats (id, started, state) VALUES (?, now(), 2) " +
                    "ON CONFLICT(id) DO UPDATE SET started = now(), state = 2"
        )
        st.setInt(1, chatId)
        st.executeUpdate()
    }

    fun getAdminGroups(login: String): List<Group> = withConnection {
        val st = prepareStatement("SELECT * FROM groups WHERE author = ?")
        st.setString(1, login)
        val results = st.executeQuery()

        val ret = mutableListOf<Group>()
        while (results.next()) {
            ret += Group(results)
        }
        st.close()

        ret
    }

    private fun <T> withConnection(action: Connection.() -> T): T {
        DriverManager.getConnection(dbUrl)!!.use {
            return it.action()
        }
    }
}

class Group(results: ResultSet) {
    val id = results.getInt("id")
    val name = results.getString("name")
    val authorLogin = results.getString("author")
    val membersNum = results.getInt("memberNum")
}
