package az.santabot.model

import java.sql.ResultSet

class Group(
    val id: Int,
    val name: String,
    val authorName: String,
    val membersNum: Int,
    val closed: Boolean
) {
    constructor(results: ResultSet) : this(
        id = results.getInt("id"),
        name = results.getString("name")!!,
        authorName = results.getString("author_name")!!,
        membersNum = results.getInt("memberNum"),
        closed = results.getBoolean("closed")
    )
}