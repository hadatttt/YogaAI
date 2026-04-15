package com.hadat.aiyoga.data.firestore.repository

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.hadat.aiyoga.data.firestore.model.LikeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class LikeRepository {
    private val db = FirebaseFirestore.getInstance()
    private val likesCollection = db.collection("likes")
    private val sequenceCollection = db.collection("yoga_sequences")

    companion object {
        private const val TAG = "LikeRepository"
    }
    suspend fun increaseViewCount(sequenceId: String) = withContext(Dispatchers.IO) {
        try {
            sequenceCollection.document(sequenceId)
                .update("viewCount", FieldValue.increment(1))
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi tăng viewCount: ${e.message}")
        }
    }
    suspend fun toggleLike(sequenceId: String, userId: String): Boolean? = withContext(Dispatchers.IO) {
        try {
            val snapshot = likesCollection
                .whereEqualTo("sequenceId", sequenceId)
                .whereEqualTo("userId", userId)
                .get()
                .await()

            val sequenceRef = sequenceCollection.document(sequenceId)
            var isLiked: Boolean? = null

            db.runTransaction { transaction ->
                if (snapshot.isEmpty) {
                    val newLikeRef = likesCollection.document()
                    val likeObj = LikeModel(
                        userId = userId,
                        sequenceId = sequenceId
                    )
                    transaction.set(newLikeRef, likeObj)

                    transaction.update(sequenceRef, "likeCount", FieldValue.increment(1))
                    isLiked = true
                } else {
                    for (doc in snapshot.documents) {
                        transaction.delete(doc.reference)
                    }
                    transaction.update(sequenceRef, "likeCount", FieldValue.increment(-1))
                    isLiked = false
                }
            }.await()

            isLiked
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi toggleLike: ${e.message}")
            null
        }
    }

    suspend fun checkIsLiked(sequenceId: String, userId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val snapshot = likesCollection
                .whereEqualTo("sequenceId", sequenceId)
                .whereEqualTo("userId", userId)
                .get()
                .await()
            !snapshot.isEmpty
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getLikedSequenceIds(userId: String): List<String> = withContext(Dispatchers.IO) {
        try {
            val snapshot = likesCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()

            val likeList = snapshot.toObjects(LikeModel::class.java)
            likeList.map { it.sequenceId }
        } catch (e: Exception) {
            emptyList()
        }
    }
}