package com.hadat.aiyoga.data.firestore.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.hadat.aiyoga.sequence.WorkoutSequenceModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Date

class SequenceRepository {
    private val db = FirebaseFirestore.getInstance()
    private val sequenceCollection = db.collection("yoga_sequences")

    private fun isRealUser(userId: String): Boolean = userId.isNotBlank() && userId != "guest"

    suspend fun saveSequence(sequence: WorkoutSequenceModel): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val docRef = sequenceCollection.document()

            val data = hashMapOf(
                "id" to docRef.id,
                "userId" to sequence.userId,
                "title" to sequence.title,
                "coverImageUrl" to sequence.coverImageUrl,
                "totalDuration" to sequence.totalDuration,
                "level" to sequence.level,
                "isPublic" to sequence.isPublic,
                "poses" to sequence.poses,
                "likeCount" to 0,
                "viewCount" to 0,
                "createdAt" to FieldValue.serverTimestamp() // 🔥 QUAN TRỌNG
            )

            docRef.set(data).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun getMySequences(userId: String): List<WorkoutSequenceModel> = withContext(Dispatchers.IO) {
        return@withContext try {
            sequenceCollection
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
                .toObjects(WorkoutSequenceModel::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getMySequencesInRange(userId: String, fromMillis: Long, toMillis: Long): List<WorkoutSequenceModel> = withContext(Dispatchers.IO) {
        return@withContext try {
            sequenceCollection
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("createdAt", Date(fromMillis))
                .whereLessThanOrEqualTo("createdAt", Date(toMillis))
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .get()
                .await()
                .toObjects(WorkoutSequenceModel::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getAllSequences(): List<WorkoutSequenceModel> = withContext(Dispatchers.IO) {
        return@withContext try {
            sequenceCollection
                .whereEqualTo("isPublic", true)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
                .toObjects(WorkoutSequenceModel::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getCommunitySequences(excludeUserId: String): List<WorkoutSequenceModel> = withContext(Dispatchers.IO) {
        return@withContext try {
            getAllSequences().filterNot { isRealUser(excludeUserId) && it.userId == excludeUserId }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getTopLikedCommunitySequences(excludeUserId: String, limit: Long = 10): List<WorkoutSequenceModel> =
        withContext(Dispatchers.IO) {
            try {
                getTopLikedSequences(limit = 200)
                    .filterNot { isRealUser(excludeUserId) && it.userId == excludeUserId }
                    .take(limit.toInt())
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }

    suspend fun getTrendingCommunitySequences(excludeUserId: String, limit: Long = 10): List<WorkoutSequenceModel> =
        withContext(Dispatchers.IO) {
            try {
                getTrendingSequences(limit = 200)
                    .filterNot { isRealUser(excludeUserId) && it.userId == excludeUserId }
                    .take(limit.toInt())
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }

    suspend fun getSequenceById(sequenceId: String): WorkoutSequenceModel? = withContext(Dispatchers.IO) {
        return@withContext try {
            if (sequenceId.isBlank()) return@withContext null
            sequenceCollection.document(sequenceId).get().await().toObject(WorkoutSequenceModel::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    suspend fun getTrendingSequences(limit: Long = 10): List<WorkoutSequenceModel> = withContext(Dispatchers.IO) {
        return@withContext try {
            sequenceCollection
                .whereEqualTo("isPublic", true)
                .orderBy("viewCount", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .await()
                .toObjects(WorkoutSequenceModel::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }
    suspend fun updateSequence(sequence: WorkoutSequenceModel): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            if (sequence.id.isEmpty()) return@withContext false

            val updateData = hashMapOf(
                "title" to sequence.title,
                "coverImageUrl" to sequence.coverImageUrl,
                "totalDuration" to sequence.totalDuration,
                "level" to sequence.level,
                "isPublic" to sequence.isPublic,
                "poses" to sequence.poses
            )

            sequenceCollection.document(sequence.id)
                .set(updateData, SetOptions.merge())
                .await()

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    suspend fun getTopLikedSequences(limit: Long = 10): List<WorkoutSequenceModel> = withContext(Dispatchers.IO) {
        return@withContext try {
            sequenceCollection
                .whereEqualTo("isPublic", true)
                .orderBy("likeCount", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .await()
                .toObjects(WorkoutSequenceModel::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}