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

    fun createGroupInChat(chatId: Int, name: String): Int {
        return withConnection {
            val chatStatement = prepareStatement("UPDATE chats SET state=1 WHERE id=?")
            chatStatement.setInt(1, chatId)
            chatStatement.executeUpdate()

            val groupStatement = prepareStatement("INSERT INTO groups(name) VALUES (?)")
            groupStatement.setString(1, name)
            groupStatement.executeUpdate()
            val keys = groupStatement.generatedKeys
            keys.next()
            keys.getInt(1)
        }
    }

    private fun <T> withConnection(action: Connection.() -> T): T {
        DriverManager.getConnection(dbUrl)!!.use {
            return it.action()
        }
    }
}

class GroupAnswer(val id: Int, val name: String)
