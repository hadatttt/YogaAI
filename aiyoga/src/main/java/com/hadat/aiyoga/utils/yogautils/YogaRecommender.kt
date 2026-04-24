package com.hadat.aiyoga.utils.yogautils

import com.hadat.aiyoga.sequence.SequenceModel
import com.hadat.aiyoga.yogamain.YogaPoseModel

class YogaRecommender {
    fun getRecommendations(
        currentSequence: List<SequenceModel>,
        allPoses: List<YogaPoseModel>,
        limit: Int = 5
    ): List<YogaPoseModel> {
        if (allPoses.isEmpty()) return emptyList()

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