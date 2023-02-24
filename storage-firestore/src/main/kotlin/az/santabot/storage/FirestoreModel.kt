package az.santabot.storage

import az.santabot.model.Group
import com.google.cloud.firestore.Firestore

internal fun Group.toFirebase() = mapOf(
    "gid" to id,
    "name" to name,
    "authorName" to authorName,
    "membersNum" to membersNum,
    "closed" to closed
)

internal fun Group.save(db: Firestore): Int {
    val id = FirestoreUtil.nextId(db, "groups").toInt()

    db.collection("groups")
        .document(id.toString())
        .set(toFirebase())
        .get()

    return id
}
