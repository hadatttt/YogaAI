package com.hadat.aiyoga.data.firestore.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.hadat.aiyoga.singleyoga.WorkoutResultModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
class WorkoutRepository {

    private val db = FirebaseFirestore.getInstance()
    private val workoutCollection = db.collection(COLLECTION_WORKOUT_RESULTS)

    companion object {
        private const val COLLECTION_WORKOUT_RESULTS = "yoga_workout_results"
    }

    suspend fun saveWorkoutResult(result: WorkoutResultModel): Boolean {
        return try {
            workoutCollection.add(result).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun getWorkoutHistory(userId: String): List<WorkoutResultModel> =
        withContext(Dispatchers.IO) {
            try {
                workoutCollection
                    .whereEqualTo("userId", userId)
                    .orderBy("workoutTimestamp", Query.Direction.DESCENDING)
                    .get()
                    .await()
                    .toObjects(WorkoutResultModel::class.java)
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    suspend fun getHistoryInRange(
        userId: String,
        fromMillis: Long,
        toMillis: Long
    ): List<WorkoutResultModel> = withContext(Dispatchers.IO) {
        return@withContext try {
            workoutCollection
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("workoutTimestamp", fromMillis)
                .whereLessThanOrEqualTo("workoutTimestamp", toMillis)
                .orderBy("workoutTimestamp", Query.Direction.DESCENDING)
                .get()
                .await()
                .toObjects(WorkoutResultModel::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}