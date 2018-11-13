package az.santabot

import java.sql.Connection
import java.sql.DriverManager


class DbService {
    private val dbUrl = System.getenv("JDBC_DATABASE_URL")

    fun getUrlInfo(): String = "JDU:${System.getenv("JDBC_DATABASE_URL")}, DU:${System.getenv("DATABASE_URL")}"

    private fun <T : Any> withConnection(action: Connection.() -> T): T {
        val connection = DriverManager.getConnection(dbUrl)!!
        val ret = connection.action()
        connection.close()
        return ret
    }
}