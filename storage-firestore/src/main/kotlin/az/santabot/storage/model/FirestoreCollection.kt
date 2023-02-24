package az.santabot.storage.model

import az.santabot.storage.FirestoreUtil
import com.google.cloud.firestore.*

typealias QuerySetup = CollectionReference.() -> Query

abstract class FirestoreCollection<T>(val name: String) {
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
            ?.toModel()

    fun save(value: T, tx: Transaction? = null, id: String? = null): String {
        val col = db.collection(name)
        val idToUpdate = id ?: value.naturalId() ?: FirestoreUtil.nextId(db, tx, name).toString()
        val doc = value.toFirebase()

        val docRef = col.document(idToUpdate)
        tx?.set(docRef, doc) ?: docRef.set(doc)

        return idToUpdate
    }

    fun remove(tx: Transaction?, querySetup: QuerySetup) {
        db.collection(name)
            .querySetup()
            .let { query -> tx?.get(query) ?: query.get() }
            .get()
            .forEach { doc ->
                val docRef = doc.reference
                tx?.delete(docRef) ?: docRef.delete()
            }
    }

    fun update(tx: Transaction?, querySetup: QuerySetup, updates: Map<String, Any?>) {
        tx?.let { updateTx(tx, querySetup, updates) }
            ?: db.runTransaction { updateTx(it, querySetup, updates) }
    }

    private fun updateTx(tx: Transaction, querySetup: QuerySetup, updates: Map<String, Any?>) {
        tx.get(collection.querySetup())
            .get()
            .forEach { doc ->
                tx.update(doc.reference, updates)
            }
    }
}