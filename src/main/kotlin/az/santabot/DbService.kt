package az.santabot

class DbService {
    fun getUrlInfo(): String = "JDU:${System.getenv("JDBC_DATABASE_URL")}, DU:${System.getenv("DATABASE_URL")}"
}