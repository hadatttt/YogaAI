package com.hadat.aiyoga.utils.yogautils


import com.hadat.aiyoga.data.remoteconfig.YogaDataUtils
import com.hadat.aiyoga.yoga_single.WorkoutResultModel
import kotlin.math.pow

object HealthCalculatorUtils {

    /**
     * Tính BMI
     * Công thức: Weight (kg) / Height (m)^2
     */
    fun calculateBMI(weight: Float, heightCm: Float): Float {
        if (heightCm <= 0) return 0f
        val heightMeters = heightCm / 100
        return weight / heightMeters.pow(2)
    }

    /**
     * Tính BMR (Mifflin-St Jeor Equation)
     * Nam: 10*weight + 6.25*height - 5*age + 5
     * Nữ: 10*weight + 6.25*height - 5*age - 161
     */
    fun calculateBMR(gender: String, age: Int, weight: Float, heightCm: Float): Float {
        val base = (10 * weight) + (6.25f * heightCm) - (5 * age)
        return if (gender.equals("Male", ignoreCase = true)) {
            base + 5
        } else {
            base - 161
        }
    }

    /**
     * Tính TDEE (Total Daily Energy Expenditure)
     * @param activityLevelPos: 0: Sedentary, 1: Light, 2: Moderate, 3: Very Active
     */
    fun calculateTDEE(bmr: Float, activityLevelPos: Int): Float {
        val multiplier = when (activityLevelPos) {
            0 -> 1.2f   // Sedentary
            1 -> 1.375f // Lightly Active
            2 -> 1.55f  // Moderately Active
            3 -> 1.725f // Very Active
            else -> 1.2f
        }
        return bmr * multiplier
    }

    /**
     * Tính Calo tiêu thụ cho một bài tập (Dựa trên MET)
     * Công thức: (MET * 3.5 * Weight * DurationMinutes) / 200
     */
    fun calculateDailyGoalCalories(tdee: Float): Int {
        return tdee.toInt()
    }
    fun calculateWorkoutCaloriesByMet(
        met: Double,
        weight: Float,
        durationSec: Int
    ): Float {
        val durationMinutes = durationSec / 60f

        return ((met * 3.5 * weight * durationMinutes) / 200).toFloat()
    }

    fun calculateTotalCalories(
        workouts: List<WorkoutResultModel>,
        weight: Float
    ): Float {
        if (workouts.isEmpty()) return 0f

        return workouts.sumOf { workout ->
            val met = YogaDataUtils.getMetValue(workout.poseId)
            calculateWorkoutCaloriesByMet(
                met = met,
                weight = weight,
                durationSec = workout.durationInSeconds
            ).toDouble()
        }.toFloat()
    }
    /**
     * Tính Accuracy (%)
     */
    fun calculateAccuracy(
        expectedTimeSec: Int,
        wrongCount: Int
    ): Float {
        if (expectedTimeSec <= 0) return 0f
        val baseScore = 100f
        val timeMinutes = expectedTimeSec / 60f
        val penaltyPerWrong = 12f / (1f + timeMinutes)
        val penalty = wrongCount * penaltyPerWrong
        return (baseScore - penalty).coerceIn(0f, 100f)
    }
}