package az.santabot.storage

import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.Transaction

internal object FirestoreUtil {
    fun nextId(db: Firestore, tx: Transaction?, name: String): Long =
        if (tx == null) db.runTransaction { nextIdTx(db, it, name) }.get()
        else nextIdTx(db, tx, name)

    private fun nextIdTx(db: Firestore, tx: Transaction, name: String): Long {
        val docRef = db.collection("counters").document(name)
        val count = tx.get(docRef).get().getLong("count") ?: 0
        val id = count + 1
        tx.update(docRef, mapOf("count" to id))
        return id
    }
}