package az.santabot.storage

import com.google.api.core.ApiFuture
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.Transaction

internal typealias FStatement = (Transaction) -> Unit

internal class TxContext(val tx: Transaction) {
    val delayedWrites = mutableListOf<FStatement>()

    operator fun plusAssign(statement: FStatement) {
        delayedWrites += statement
    }

    fun runWrites() =
        delayedWrites.forEach { it.invoke(tx) }
}

internal object FirestoreUtil {
    /**
     * Beware! Does not support several IDs for the same collection in one transaction.
     */
    fun nextId(db: Firestore, ctx: TxContext?, name: String): Long =
        if (ctx == null) db.runTransaction { nextIdTx(db, it, name) }.get()
        else nextIdCtx(db, ctx, name)

    private fun nextIdTx(db: Firestore, tx: Transaction, name: String): Long {
        val docRef = db.collection("counters").document(name)
        val count = tx.get(docRef).get().getLong("count") ?: 0
        val id = count + 1
        tx.set(docRef, mapOf("count" to id))
        return id
    }

    private fun nextIdCtx(db: Firestore, ctx: TxContext, name: String): Long {
        val docRef = db.collection("counters").document(name)
        val count = ctx.tx.get(docRef).get().getLong("count") ?: 0
        val id = count + 1
        ctx += { tx -> tx.set(docRef, mapOf("count" to id)) }
        return id
    }
}

internal fun <T> Firestore.tx(block: (TxContext) -> T): ApiFuture<T> {
    return runTransaction { tr ->
        val ctx = TxContext(tr)
        val ret = block(ctx)
        ctx.runWrites()
        ret
    }
}