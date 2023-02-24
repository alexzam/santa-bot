package az.santabot.storage.model

import az.santabot.model.Group

class DbGroup(
    val id: Int,
    val name: String,
    val authorName: String,
    val membersNum: Int,
    val closed: Boolean,
    val uids: List<Int>
) {
    fun toModel() = Group(
        id = id,
        name = name,
        authorName = authorName,
        membersNum = membersNum,
        closed = closed
    )

    fun withUids(newUids: List<Int>) = DbGroup(
        id = id,
        name = name,
        authorName = authorName,
        membersNum = newUids.size,
        closed = closed,
        uids = newUids
    )

    fun withClosed(closed: Boolean) = DbGroup(
        id = id,
        name = name,
        authorName = authorName,
        membersNum = membersNum,
        closed = closed,
        uids = uids
    )
}