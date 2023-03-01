package az.santabot.storage

import com.google.api.core.ApiFutureCallback
import com.google.api.core.ApiFutures
import com.google.cloud.firestore.Query
import com.google.cloud.firestore.QueryDocumentSnapshot
import com.google.cloud.firestore.QuerySnapshot
import com.google.common.util.concurrent.MoreExecutors
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

suspend fun Query.await(): List<QueryDocumentSnapshot> {
    val value = suspendCoroutine<QuerySnapshot> { continuation ->
        val future = get()
        ApiFutures.addCallback(future, makeCallback(continuation), MoreExecutors.directExecutor())
    }

    return value.documents
}

private fun <T> makeCallback(continuation: Continuation<T>) = object : ApiFutureCallback<T> {
    override fun onFailure(t: Throwable?) {
        continuation.resumeWithException(t!!)
    }

    override fun onSuccess(result: T) {
        continuation.resume(result)
    }
}