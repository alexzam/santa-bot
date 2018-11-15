package az.santabot

import java.sql.Connection
import java.sql.DriverManager


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
                ret += GroupAnswer(results.getInt("id"), results.getString("name"))
            }
            statement.close()

            ret
        }
    }

    fun saveStartChat(id: Int) {
        withConnection {
            val statement = prepareStatement("INSERT INTO chats(id, started) VALUES (?, NOW())")
            statement.setInt(1, id)
            statement.executeUpdate()
        }
    }

    fun findChatState(id: Int): Int {
        return withConnection {
            val statement = prepareStatement("SELECT state FROM chats WHERE id = ?")
            statement.setInt(1, id)
            statement.executeQuery().getInt(1)
        }
    }

    fun createGroupInChat(chatId: Int, name: String): Int {
        return withConnection {
            val chatStatement = prepareStatement("UPDATE chats SET state=1 WHERE id=?")
            chatStatement.setInt(1, chatId)
            chatStatement.executeUpdate()

            val groupStatement = prepareStatement("INSERT INTO groups(name) VALUES (?)")
            groupStatement.setString(1, name)
            groupStatement.executeUpdate()
            groupStatement.generatedKeys.getInt(1)
        }
    }

    private fun <T : Any> withConnection(action: Connection.() -> T): T {
        val connection = DriverManager.getConnection(dbUrl)!!
        val ret = connection.action()
        connection.close()
        return ret
    }
}

class GroupAnswer(val id: Int, val name: String)
