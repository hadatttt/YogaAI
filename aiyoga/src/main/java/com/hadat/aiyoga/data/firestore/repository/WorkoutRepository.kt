package com.hadat.aiyoga.data.firestore.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.hadat.aiyoga.data.remoteconfig.YogaDataUtils
import com.hadat.aiyoga.yoga_single.WorkoutResultModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class WorkoutRepository {

    private val db = FirebaseFirestore.getInstance()
    private val workoutCollection = db.collection(COLLECTION_WORKOUT_RESULTS)

    companion object {
        private const val COLLECTION_WORKOUT_RESULTS = "yoga_workout_results"
    }

    suspend fun saveWorkoutResult(result: WorkoutResultModel): Boolean = withContext(Dispatchers.IO) {
        try {
            val resultToSave = result.copy(poseName = "", poseUrl = "")
            workoutCollection.add(resultToSave).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getWorkoutHistory(userId: String): List<WorkoutResultModel> = withContext(Dispatchers.IO) {
        try {
            val snapshots = workoutCollection
                .whereEqualTo("userId", userId)
                .orderBy("workoutTimestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            snapshots.toObjects(WorkoutResultModel::class.java).map { mapMetadata(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getHistoryInRange(
        userId: String,
        fromMillis: Long,
        toMillis: Long
    ): List<WorkoutResultModel> = withContext(Dispatchers.IO) {
        try {
            val snapshots = workoutCollection
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("workoutTimestamp", fromMillis)
                .whereLessThanOrEqualTo("workoutTimestamp", toMillis)
                .orderBy("workoutTimestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            snapshots.toObjects(WorkoutResultModel::class.java).map { mapMetadata(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun mapMetadata(result: WorkoutResultModel): WorkoutResultModel {
        val poseInfo = YogaDataUtils.getPoseById(result.poseId)
        return result.copy(
            poseName = poseInfo?.name ?: "Unknown",
            poseUrl = poseInfo?.photo_url ?: ""
        )
    }
}