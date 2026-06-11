package com.hadat.aiyoga.utils.yogautils

import android.util.Log
import com.hadat.aiyoga.data.api.RecommendRequest
import com.hadat.aiyoga.data.api.RetrofitClient
import com.hadat.aiyoga.sequence.SequenceModel
import com.hadat.aiyoga.yoga_ai.YogaPoseModel

class YogaRecommender {
    suspend fun getRecommendations(
        currentSequence: List<SequenceModel>,
        allPoses: List<YogaPoseModel>,
        limit: Int = 5
    ): List<YogaPoseModel> {
        if (allPoses.isEmpty()) return emptyList()

        val apiRecommendations = getApiRecommendations(currentSequence, allPoses, limit)
        if (apiRecommendations.isNotEmpty()) return apiRecommendations

        return getLocalRecommendations(currentSequence, allPoses, limit)
    }

    private suspend fun getApiRecommendations(
        currentSequence: List<SequenceModel>,
        allPoses: List<YogaPoseModel>,
        limit: Int
    ): List<YogaPoseModel> {
        val existingIds = currentSequence.map { it.id }.toSet()
        if (existingIds.isEmpty()) return emptyList()

        return try {
            val response = RetrofitClient.apiService.recommend(
                RecommendRequest(ids = existingIds.toList())
            )

            val poseById = allPoses.associateBy { it.id }
            response.ids
                .asSequence()
                .filterNot { it in existingIds }
                .distinct()
                .mapNotNull { poseById[it] }
                .take(limit)
                .toList()
        } catch (e: Exception) {
            Log.w("YogaRecommender", "Recommend API failed, fallback to local", e)
            emptyList()
        }
    }

    private fun getLocalRecommendations(
        currentSequence: List<SequenceModel>,
        allPoses: List<YogaPoseModel>,
        limit: Int
    ): List<YogaPoseModel> {
        val lastPose = currentSequence.lastOrNull()

        if (lastPose == null) {
            return allPoses.filter {
                it.category.equals("Seated", true) ||
                        it.category.equals("Arm Leg Support", true)
            }.shuffled().take(limit)
        }

        val targetCategories = when (lastPose.category) {
            "Standing" -> listOf("Standing", "Arm Balance", "Inversion")
            "Seated" -> listOf("Seated", "Prone", "Supine")
            "Prone" -> listOf("Prone", "Arm Leg Support", "Seated")
            "Supine" -> listOf("Supine", "Seated", "Inversion")
            "Arm Balance" -> listOf("Inversion", "Standing", "Arm Leg Support")
            "Inversion" -> listOf("Supine", "Seated")
            "Arm Leg Support" -> listOf("Standing", "Prone", "Arm Balance")
            else -> listOf("Seated", "Standing", "Supine")
        }

        val existingIds = currentSequence.map { it.id }

        return allPoses
            .filter { pose ->
                !existingIds.contains(pose.id) &&
                        targetCategories.any { cat -> pose.category.equals(cat, true) }
            }
            .shuffled()
            .take(limit)
    }
}
