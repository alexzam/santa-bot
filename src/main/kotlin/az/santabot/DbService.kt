package az.santabot

import java.sql.Connection
import java.sql.DriverManager


class DbService {
    private val dbUrl = System.getenv("JDBC_DATABASE_URL")

    fun getUrlInfo(): String = "JDU:${System.getenv("JDBC_DATABASE_URL")}, DU:${System.getenv("DATABASE_URL")}"

    fun getGroups(uid: Int): List<GroupAnswer> {
        return withConnection {
            val statement =
                prepareStatement("SELECT g.* FROM groups AS g JOIN user_groups AS ug ON ug.gid=g.id WHERE ug.uid=%")
            statement.setInt(1, uid)
            val results = statement.executeQuery()

            val ret = mutableListOf<GroupAnswer>()
            results.beforeFirst()
            while (results.next()) {
                ret += GroupAnswer(results.getInt("id"), results.getString("name"))
            }
            statement.close()

            ret
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
