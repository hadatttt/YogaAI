package com.hadat.aiyoga.data.firestore.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.hadat.aiyoga.data.remoteconfig.YogaDataUtils
import com.hadat.aiyoga.sequence.WorkoutSequenceModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class SequenceRepository {
    private val db = FirebaseFirestore.getInstance()
    private val sequenceCollection = db.collection(COLLECTION_SEQUENCES)

    companion object {
        private const val COLLECTION_SEQUENCES = "yoga_sequences"
    }

    private fun isRealUser(userId: String): Boolean = userId.isNotBlank() && userId != "guest"

    suspend fun saveSequence(sequence: WorkoutSequenceModel): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val docRef = if (sequence.id.isEmpty()) sequenceCollection.document() else sequenceCollection.document(sequence.id)
            sequence.id = docRef.id

            // Tối ưu: Chỉ lưu ID và Duration, xóa các field còn lại để nhẹ Database
            val cleanedPoses = sequence.poses.map {
                it.copy(name = "", photoUrl = "", category = "")
            }

            val data = hashMapOf(
                "id" to docRef.id,
                "userId" to sequence.userId,
                "authorName" to sequence.authorName,
                "title" to sequence.title,
                "coverImageUrl" to sequence.coverImageUrl,
                "totalDuration" to sequence.totalDuration,
                "level" to sequence.level,
                "isPublic" to sequence.isPublic,
                "poses" to cleanedPoses,
                "likeCount" to sequence.likeCount,
                "viewCount" to sequence.viewCount,
                "createdAt" to (sequence.createdAt ?: FieldValue.serverTimestamp())
            )

            docRef.set(data, SetOptions.merge()).await()
            true
        } catch (e: Exception) {
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
                .map { mapSequenceMetadata(it) }
        } catch (e: Exception) {
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
                .map { mapSequenceMetadata(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getCommunitySequences(excludeUserId: String): List<WorkoutSequenceModel> = withContext(Dispatchers.IO) {
        return@withContext try {
            getAllSequences().filterNot { isRealUser(excludeUserId) && it.userId == excludeUserId }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getTopLikedCommunitySequences(excludeUserId: String, limit: Long = 10): List<WorkoutSequenceModel> = withContext(Dispatchers.IO) {
        try {
            getTopLikedSequences(limit = 200)
                .filterNot { isRealUser(excludeUserId) && it.userId == excludeUserId }
                .take(limit.toInt())
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getTrendingCommunitySequences(excludeUserId: String, limit: Long = 10): List<WorkoutSequenceModel> = withContext(Dispatchers.IO) {
        try {
            getTrendingSequences(limit = 200)
                .filterNot { isRealUser(excludeUserId) && it.userId == excludeUserId }
                .take(limit.toInt())
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getSequenceById(sequenceId: String): WorkoutSequenceModel? = withContext(Dispatchers.IO) {
        return@withContext try {
            if (sequenceId.isBlank()) return@withContext null
            val sequence = sequenceCollection.document(sequenceId).get().await().toObject(WorkoutSequenceModel::class.java)
            sequence?.let { mapSequenceMetadata(it) }
        } catch (e: Exception) {
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
                .map { mapSequenceMetadata(it) }
        } catch (e: Exception) {
            emptyList()
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
                .map { mapSequenceMetadata(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun updateSequence(sequence: WorkoutSequenceModel): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            if (sequence.id.isEmpty()) return@withContext false
            saveSequence(sequence) // Tận dụng logic cleanedPoses ở hàm save
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteSequence(sequenceId: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            if (sequenceId.isBlank()) return@withContext false
            sequenceCollection.document(sequenceId).delete().await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun updateSequenceVisibility(sequenceId: String, isPublic: Boolean): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            if (sequenceId.isBlank()) return@withContext false
            sequenceCollection.document(sequenceId)
                .update("isPublic", isPublic)
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun mapSequenceMetadata(sequence: WorkoutSequenceModel): WorkoutSequenceModel {
        val mappedPoses = sequence.poses.map { pose ->
            val poseInfo = YogaDataUtils.getPoseById(pose.id)
            pose.copy(
                name = poseInfo?.name ?: "Unknown",
                photoUrl = poseInfo?.photo_url ?: "",
                category = poseInfo?.category ?: ""
            )
        }
        return sequence.copy(poses = mappedPoses)
    }
}
