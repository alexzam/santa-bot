package az.santabot.model

import java.sql.ResultSet

class DbUser(results: ResultSet) {
    private val name: String? = results.getString("target_name")
    private val username: String? = results.getString("target_username")

    override fun toString(): String =
        if (name != null && username != null) "$name (@$username)"
        else if (username != null) "@$username"
        else name ?: "Fully anonymous user"
}