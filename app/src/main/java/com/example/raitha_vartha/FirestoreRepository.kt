package com.example.raitha_vartha

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import java.util.UUID

class FirestoreRepository(private val context: Context) {
    private val db by lazy { 
        FirebaseFirestore.getInstance().apply {
            firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(PersistentCacheSettings.newBuilder()
                    .setSizeBytes(100 * 1024 * 1024)
                    .build())
                .build()
        }
    }
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val storage by lazy { FirebaseStorage.getInstance() }
    private val tipsCollection by lazy { db.collection("tips") }
    private val usersCollection by lazy { db.collection("users") }

    private suspend fun <T> retryFirebase(
        times: Int = 5,
        initialDelay: Long = 1500,
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelay
        repeat(times - 1) { attempt ->
            try {
                return withTimeout(60000) { block() }
            } catch (e: Exception) {
                Log.w("FirestoreRepository", "Attempt ${attempt + 1} failed: ${e.message}")
                delay(currentDelay)
                currentDelay *= 2
            }
        }
        return withTimeout(60000) { block() }
    }

    suspend fun uploadImage(uri: Uri, folder: String): String {
        val fileName = "${folder}/${UUID.randomUUID()}.jpg"
        val ref = storage.reference.child(fileName)
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }

    fun getAllTips(): Flow<List<TipEntity>> = callbackFlow {
        val subscription = tipsCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("Firestore", "Tips snapshot error", error)
                return@addSnapshotListener
            }
            val tips = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(TipEntity::class.java)?.copy(id = doc.id)
            } ?: emptyList()
            trySend(tips)
        }
        awaitClose { subscription.remove() }
    }

    suspend fun registerUser(user: UserEntity): Result<Unit> {
        return try {
            retryFirebase {
                auth.createUserWithEmailAndPassword(user.email, user.password).await()
                usersCollection.document(user.email).set(user).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginUser(identifier: String, password: String): Result<UserEntity> {
        return try {
            val user = retryFirebase {
                val isEmail = identifier.contains("@")
                if (isEmail) {
                    auth.signInWithEmailAndPassword(identifier, password).await()
                    usersCollection.document(identifier).get().await().toObject(UserEntity::class.java)
                } else {
                    val query = usersCollection.whereEqualTo("phoneNumber", identifier).get().await()
                    val userDoc = query.documents.firstOrNull()
                    val u = userDoc?.toObject(UserEntity::class.java)
                    if (u != null) {
                        auth.signInWithEmailAndPassword(u.email, password).await()
                        u
                    } else null
                }
            }
            if (user != null) Result.success(user) else Result.failure(Exception("User not found"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun insertTip(tip: TipEntity) {
        try { retryFirebase { tipsCollection.document(tip.id.ifEmpty { tipsCollection.document().id }).set(tip).await() } } catch (e: Exception) {}
    }

    suspend fun deleteTip(tipId: String) {
        try { retryFirebase { tipsCollection.document(tipId).delete().await() } } catch (e: Exception) {}
    }

    suspend fun updateUser(user: UserEntity) {
        try { retryFirebase { usersCollection.document(user.email).set(user).await() } } catch (e: Exception) {}
    }

    suspend fun updatePassword(email: String, newPassword: String) {
        try { auth.currentUser?.updatePassword(newPassword)?.await(); usersCollection.document(email).update("password", newPassword).await() } catch (e: Exception) {}
    }

    fun getAllUsers(): Flow<List<UserEntity>> = callbackFlow {
        val subscription = usersCollection.addSnapshotListener { snapshot, _ ->
            val users = snapshot?.documents?.mapNotNull { it.toObject(UserEntity::class.java) } ?: emptyList()
            trySend(users)
        }
        awaitClose { subscription.remove() }
    }

    suspend fun toggleLike(tipId: String, userEmail: String) {
        val doc = tipsCollection.document(tipId).get().await()
        val likedBy = doc.get("likedBy") as? List<*> ?: emptyList<String>()
        if (likedBy.contains(userEmail)) {
            tipsCollection.document(tipId).update("likedBy", FieldValue.arrayRemove(userEmail), "likesCount", FieldValue.increment(-1)).await()
        } else {
            tipsCollection.document(tipId).update("likedBy", FieldValue.arrayUnion(userEmail), "likesCount", FieldValue.increment(1)).await()
        }
    }

    fun getComments(tipId: String): Flow<List<CommentEntity>> = callbackFlow {
        val sub = tipsCollection.document(tipId).collection("comments").orderBy("timestamp").addSnapshotListener { snapshot, _ ->
            val list = snapshot?.documents?.mapNotNull { it.toObject(CommentEntity::class.java) } ?: emptyList()
            trySend(list)
        }
        awaitClose { sub.remove() }
    }

    suspend fun addComment(tipId: String, comment: CommentEntity) {
        tipsCollection.document(tipId).collection("comments").add(comment).await()
    }
}
