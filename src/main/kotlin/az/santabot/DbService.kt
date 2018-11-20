package az.santabot

import az.santabot.model.User
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Statement


class DbService {
    private val dbUrl = System.getenv("JDBC_DATABASE_URL")

    fun getUrlInfo(): String = "JDU:${System.getenv("JDBC_DATABASE_URL")}, DU:${System.getenv("DATABASE_URL")}"

    fun getGroups(uid: Int): List<GroupAnswer> {
        return withConnection {
            val statement =
                prepareStatement("SELECT g.* FROM groups AS g JOIN user_groups AS ug ON ug.gid=g.id WHERE ug.uid=?")
            statement.setInt(1, uid)
            val results = statement.executeQuery()

            val ret = mutableListOf<GroupAnswer>()
            while (results.next()) {
                ret += GroupAnswer(
                    results.getInt("id"),
                    results.getString("name"),
                    results.getString("author"),
                    results.getInt("memberNum")
                )
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

    fun addToGroup(gid: Int, uid: Int) {
        return withConnection {
            try {
                val st = prepareStatement("INSERT INTO user_groups(gid, uid) VALUES (?, ?)")
                st.setInt(1, gid)
                st.setInt(2, uid)
                st.executeUpdate()

                val numSt = prepareStatement("UPDATE groups SET memberNum = memberNum + 1 WHERE id = ?")
                numSt.setInt(1, gid)
                numSt.executeUpdate()
            } catch (ignored: Exception) {
            }
        }
    }

    private fun <T> withConnection(action: Connection.() -> T): T {
        DriverManager.getConnection(dbUrl)!!.use {
            return it.action()
        }
    }
}

class GroupAnswer(
    val id: Int,
    val name: String,
    val authorLogin: String,
    val membersNum: Int
)
