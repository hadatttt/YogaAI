package com.hadat.aiyoga.data.firestore.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.hadat.aiyoga.singleyoga.WorkoutResultModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class WorkoutRepository {
    private val db = FirebaseFirestore.getInstance()
    private val workoutCollection = db.collection("workout_results")


    suspend fun saveWorkoutResult(result: WorkoutResultModel): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            workoutCollection.add(result).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    suspend fun getWorkoutHistory(userId: String): List<WorkoutResultModel> = withContext(Dispatchers.IO) {
        return@withContext try {
            workoutCollection
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
                .toObjects(WorkoutResultModel::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    suspend fun getHistoryByPose(userId: String, poseId: Int): List<WorkoutResultModel> = withContext(Dispatchers.IO) {
        return@withContext try {
            workoutCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("poseId", poseId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
                .toObjects(WorkoutResultModel::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }
}