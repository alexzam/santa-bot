package az.santabot.storage.model

import az.santabot.storage.FirestoreUtil
import az.santabot.storage.TxContext
import az.santabot.storage.tx
import com.google.cloud.firestore.*

typealias QuerySetup = CollectionReference.() -> Query

abstract internal class FirestoreCollection<T>(val name: String) {
    lateinit var db: Firestore
    private val collection get() = db.collection(name)

    abstract fun DocumentSnapshot.toModel(): T
    abstract fun T.toFirebase(): Map<String, Any?>
    open fun T.naturalId(): String? = null

    private fun DocumentSnapshot.throwNoField(name: String): Nothing =
        throw Exception("No $name field in ${reference.path}")

    protected fun DocumentSnapshot.eGetInt(name: String) =
        getLong(name)?.toInt() ?: throwNoField(name)

    protected fun DocumentSnapshot.eGetString(name: String) =
        getString(name) ?: throwNoField(name)

    protected fun DocumentSnapshot.eGetBoolean(name: String) =
        getBoolean(name) ?: throwNoField(name)

    protected fun <V> DocumentSnapshot.eGetArray(name: String): List<V> =
        (get(name) as? List<V>) ?: listOf()

    fun find(querySetup: QuerySetup): List<T> =
        db.collection(name)
            .querySetup()
            .get().get()
            .asSequence()
            .map { it.toModel() }
            .toList()

    fun findOne(tx: Transaction? = null, querySetup: QuerySetup): T? =
        db.collection(name)
            .querySetup()
            .let { query -> tx?.get(query) ?: query.get() }
            .get()
            .singleOrNull()
            ?.toModel()

    fun findById(id: String, tx: Transaction? = null): T? =
        db.collection(name)
            .document(id)
            .let { tx?.get(it) ?: it.get() }
            .get()
            .takeIf { it.exists() }
            ?.toModel()

    fun save(value: T, ctx: TxContext? = null, id: String? = null): String {
        val col = db.collection(name)
        val idToUpdate = id ?: value.naturalId() ?: FirestoreUtil.nextId(db, ctx, name).toString()
        val doc = value.toFirebase()

        val docRef = col.document(idToUpdate)
        ctx?.let { it += { tx -> tx.set(docRef, doc) } } ?: docRef.set(doc)

        return idToUpdate
    }

    fun remove(ctx: TxContext?, querySetup: QuerySetup) {
        db.collection(name)
            .querySetup()
            .let { query -> ctx?.tx?.get(query) ?: query.get() }
            .get()
            .forEach { doc ->
                val docRef = doc.reference
                ctx?.let { it += { tx -> tx.delete(docRef) } } ?: docRef.delete()
            }
    }

    fun update(ctx: TxContext?, querySetup: QuerySetup, updates: Map<String, Any?>) {
        ctx?.let { updateTx(ctx, querySetup, updates) }
            ?: db.tx { updateTx(it, querySetup, updates) }
    }

    private fun updateTx(ctx: TxContext, querySetup: QuerySetup, updates: Map<String, Any?>) {
        ctx.tx.get(collection.querySetup())
            .get()
            .forEach { doc ->
                ctx += { tx -> tx.update(doc.reference, updates) }
            }
    }
}