package com.hadat.aiyoga.data.firestore.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.hadat.aiyoga.data.firestore.model.YogaInteractionModel
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class YogaRepository {
    private val db = FirebaseFirestore.getInstance()
    private val interactionCollection = db.collection("yoga_interactions")

    private fun getCurrentDateKey(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    suspend fun trackInteraction(poseId: String, poseName: String) {
        val dateKey = getCurrentDateKey()
        val docId = "${dateKey}_${poseId}"

        val docRef = interactionCollection.document(docId)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            if (!snapshot.exists()) {
                val newInteraction = YogaInteractionModel(
                    poseId = poseId,
                    poseName = poseName,
                    clickCount = 1,
                    dateKey = dateKey
                )
                transaction.set(docRef, newInteraction)
            } else {
                val currentCount = snapshot.getLong("clickCount") ?: 0
                transaction.update(docRef, "clickCount", currentCount + 1)
                transaction.update(docRef, "lastUpdated", System.currentTimeMillis())
            }
        }.await()
    }
    suspend fun getTodayTrendingPoseId(): String? {
        return try {
            val snapshot = interactionCollection
                .whereEqualTo("dateKey", getCurrentDateKey())
                .orderBy("clickCount", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()

            if (!snapshot.isEmpty) {
                val id = snapshot.documents[0].getString("poseId")
                Log.d("YOGA_DEBUG", "Tìm thấy PoseId trending trong Firestore: $id")
                id
            } else {
                Log.d("YOGA_DEBUG", "Không tìm thấy document nào cho ngày hôm nay")
                "0"
            }
        } catch (e: Exception) {
            Log.e("YOGA_DEBUG", "Lỗi Query Firestore: ${e.message}")
            "0"
        }
    }
}