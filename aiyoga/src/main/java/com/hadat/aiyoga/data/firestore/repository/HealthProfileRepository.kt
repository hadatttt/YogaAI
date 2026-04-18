package com.hadat.aiyoga.data.firestore.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.hadat.aiyoga.data.firestore.model.HealthProfileModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class HealthProfileRepository {

    private val db = FirebaseFirestore.getInstance()
    private val profileCollection = db.collection("yoga_health_profiles")

    suspend fun saveProfile(profile: HealthProfileModel): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            profileCollection
                .document(profile.userId)
                .set(profile)
                .await()
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun getProfile(userId: String): HealthProfileModel? = withContext(Dispatchers.IO) {
        return@withContext try {
            profileCollection
                .document(userId)
                .get()
                .await()
                .toObject(HealthProfileModel::class.java)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun updateWeight(userId: String, weight: Float): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            profileCollection
                .document(userId)
                .update("weight", weight)
                .await()
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun updateHeight(userId: String, height: Float): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            profileCollection
                .document(userId)
                .update("height", height)
                .await()
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun updateActivityLevel(userId: String, activityLevel: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            profileCollection
                .document(userId)
                .update("activityLevel", activityLevel)
                .await()
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun updateGoalCalories(userId: String, dailyGoalCalories: Int): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            profileCollection
                .document(userId)
                .update("dailyGoalCalories", dailyGoalCalories)
                .await()
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun deleteProfile(userId: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            profileCollection
                .document(userId)
                .delete()
                .await()
            true
        } catch (_: Exception) {
            false
        }
    }
}