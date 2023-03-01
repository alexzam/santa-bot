package az.santabot.model

import java.sql.ResultSet

class DbUser(
    private val name: String?,
    private val username: String?
) {
    constructor(results: ResultSet) : this(
        name = results.getString("target_name"),
        username = results.getString("target_username")
    )

    override fun toString(): String =
        if (name != null && username != null) "$name (@$username)"
        else if (username != null) "@$username"
        else name ?: "Fully anonymous user"
}