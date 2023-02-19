package az.santabot.model

import java.sql.ResultSet

class Group(results: ResultSet) {
    val id = results.getInt("id")
    val name = results.getString("name")!!
    val authorName = results.getString("author_name")!!
    val membersNum = results.getInt("memberNum")
    val closed: Boolean = results.getBoolean("closed")
}