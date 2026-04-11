package com.hadat.aiyoga.utils.yogautils

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.math.*

object YogaCoachUtils {

    private var referenceData: Map<Int, Map<String, Double>>? = null
    private var poseLabels = mutableMapOf<Int, String>()

    private val smoothedAngles = mutableMapOf<String, Double>()
    private const val SMOOTH_FACTOR = 0.35f

    // ===== SCORE SMOOTH =====
    private var lastScore = 0.0

    // LANDMARK INDEX
    private const val L_SHOULDER = 11; private const val R_SHOULDER = 12
    private const val L_ELBOW = 13;    private const val R_ELBOW = 14
    private const val L_WRIST = 15;    private const val R_WRIST = 16
    private const val L_HIP = 23;      private const val R_HIP = 24
    private const val L_KNEE = 25;     private const val R_KNEE = 26
    private const val L_ANKLE = 27;    private const val R_ANKLE = 28

    fun loadReferenceData(context: Context) {
        if (referenceData != null) return

        val data = mutableMapOf<Int, Map<String, Double>>()
        val labels = mutableMapOf<Int, String>()

        try {
            val reader = BufferedReader(InputStreamReader(context.assets.open("yoga_golden_v3.csv")))
            val header = reader.readLine()?.split(",") ?: return

            reader.forEachLine { line ->
                val t = line.split(",")
                if (t.size >= header.size) {
                    val id = t[0].toIntOrNull() ?: return@forEachLine
                    labels[id] = t[1]

                    val values = mutableMapOf<String, Double>()
                    for (i in 2 until t.size) {
                        values[header[i]] = t[i].toDoubleOrNull() ?: 0.0
                    }
                    data[id] = values
                }
            }

            referenceData = data
            poseLabels = labels
            Log.d("YogaCoach", "✅ Loaded ${data.size} poses")

        } catch (e: Exception) {
            Log.e("YogaCoach", "❌ Load error: ${e.message}")
        }
    }

    // ===== ANGLE FIX =====
    private fun normalizeAngle(angle: Double): Double {
        return if (angle > 180) 360 - angle else angle
    }

    private fun angleDiff(a: Double, b: Double): Double {
        val diff = abs(a - b)
        return min(diff, 180 - diff)
    }
    fun getPoseLabels(): List<String> {
        if (poseLabels.isEmpty()) return emptyList()
        return poseLabels.keys.sorted().map { poseLabels[it] ?: "Unknown" }
    }
    private fun calculateAngleVector(
        a: NormalizedLandmark,
        b: NormalizedLandmark,
        c: NormalizedLandmark
    ): Double {

        val v1x = a.x() - b.x()
        val v1y = a.y() - b.y()

        val v2x = c.x() - b.x()
        val v2y = c.y() - b.y()

        val dot = v1x * v2x + v1y * v2y
        val mag1 = sqrt(v1x * v1x + v1y * v1y)
        val mag2 = sqrt(v2x * v2x + v2y * v2y)

        if (mag1 == 0f || mag2 == 0f) return 0.0

        val cos = (dot / (mag1 * mag2)).coerceIn(-1f, 1f)
        val angle = Math.toDegrees(acos(cos.toDouble()))

        return normalizeAngle(angle)
    }

    private fun getSmoothAngle(key: String, newAngle: Double): Double {
        val old = smoothedAngles[key] ?: newAngle
        val smooth = old + SMOOTH_FACTOR * (newAngle - old)
        smoothedAngles[key] = smooth
        return smooth
    }

    fun getCoachFeedback(poseId: Int, result: PoseLandmarkerResult): Pair<Boolean, String> {

        val landmarks = result.landmarks()
        if (landmarks.isNullOrEmpty()) {
            smoothedAngles.clear()
            return false to "Hãy đứng rõ vào khung hình"
        }

        val lm = landmarks[0]
        val poseName = poseLabels[poseId] ?: "Unknown"
        val rawRef = referenceData?.get(poseId) ?: return true to "Đang phân tích..."

        // ===== USER ANGLES =====
        val u = mapOf(
            "knee_L" to getSmoothAngle("knee_L", calculateAngleVector(lm[L_HIP], lm[L_KNEE], lm[L_ANKLE])),
            "knee_R" to getSmoothAngle("knee_R", calculateAngleVector(lm[R_HIP], lm[R_KNEE], lm[R_ANKLE])),
            "hip_L" to getSmoothAngle("hip_L", calculateAngleVector(lm[L_SHOULDER], lm[L_HIP], lm[L_KNEE])),
            "hip_R" to getSmoothAngle("hip_R", calculateAngleVector(lm[R_SHOULDER], lm[R_HIP], lm[R_KNEE])),
            "arm_body_L" to getSmoothAngle("arm_body_L", calculateAngleVector(lm[L_ELBOW], lm[L_SHOULDER], lm[L_HIP])),
            "arm_body_R" to getSmoothAngle("arm_body_R", calculateAngleVector(lm[R_ELBOW], lm[R_SHOULDER], lm[R_HIP])),
            "elbow_L" to getSmoothAngle("elbow_L", calculateAngleVector(lm[L_SHOULDER], lm[L_ELBOW], lm[L_WRIST])),
            "elbow_R" to getSmoothAngle("elbow_R", calculateAngleVector(lm[R_SHOULDER], lm[R_ELBOW], lm[R_WRIST]))
        )

        // ===== SMART SWAP =====
        fun totalDiff(a: Map<String, Double>, b: Map<String, Double>): Double {
            return a.entries.sumOf { (k, v) -> angleDiff(v, b[k] ?: 0.0) }
        }

        val swapRef = mapOf(
            "knee_L" to rawRef["knee_R"]!!,
            "knee_R" to rawRef["knee_L"]!!,
            "hip_L" to rawRef["hip_R"]!!,
            "hip_R" to rawRef["hip_L"]!!,
            "arm_body_L" to rawRef["arm_body_R"]!!,
            "arm_body_R" to rawRef["arm_body_L"]!!,
            "elbow_L" to rawRef["elbow_R"]!!,
            "elbow_R" to rawRef["elbow_L"]!!
        )

        val shouldSwap = totalDiff(u, swapRef) < totalDiff(u, rawRef)
        val ref = if (shouldSwap) swapRef else rawRef

        // ===== LOG =====
        Log.d("YogaCoach", "--- [$poseName] (Vector Mode) ---")
        u.forEach { (k, v) ->
            val t = ref[k] ?: 0.0
            Log.d("YogaCoach", "$k: Máy đo: ${"%.1f".format(v)}° | Mẫu CSV: ${"%.1f".format(t)}°")
        }

        // ===== WEIGHT =====
        val weights = mapOf(
            "knee_L" to 1.0,
            "knee_R" to 1.0,
            "hip_L" to 1.5,
            "hip_R" to 1.5,
            "arm_body_L" to 1.2,
            "arm_body_R" to 1.2,
            "elbow_L" to 0.7,
            "elbow_R" to 0.7
        )

        // ===== SCORE =====
        var totalScore = 0.0
        var totalWeight = 0.0

        u.forEach { (k, v) ->
            val target = ref[k] ?: return@forEach
            val w = weights[k] ?: 1.0

            val diff = angleDiff(v, target)
            val score = max(0.0, 1 - diff / 90.0)

            totalScore += score * w
            totalWeight += w
        }

        var finalScore = totalScore / totalWeight

        // ===== SMOOTH SCORE =====
        finalScore = lastScore + 0.3 * (finalScore - lastScore)
        lastScore = finalScore

        val percent = (finalScore * 100).toInt()

        Log.d("YogaCoach", "🔥 SCORE: $percent%")

        // ===== COACH =====
        val errors = mutableListOf<Pair<Double, String>>()

        fun check(current: Double, key: String, side: String, low: String, high: String) {
            val target = ref[key] ?: return
            val diff = angleDiff(current, target)

            if (diff > 25) {
                val displaySide = if (shouldSwap) {
                    if (side == "trái") "phải" else "trái"
                } else side

                val msg = if (current < target) low else high
                errors.add(diff to "$msg $displaySide")
            }
        }

        check(u["knee_L"]!!, "knee_L", "trái", "Mở gối", "Khép gối")
        check(u["knee_R"]!!, "knee_R", "phải", "Mở gối", "Khép gối")
        check(u["hip_L"]!!, "hip_L", "trái", "Hạ hông", "Nâng hông")
        check(u["hip_R"]!!, "hip_R", "phải", "Hạ hông", "Nâng hông")
        check(u["arm_body_L"]!!, "arm_body_L", "trái", "Nâng tay", "Hạ tay")
        check(u["arm_body_R"]!!, "arm_body_R", "phải", "Nâng tay", "Hạ tay")
        check(u["elbow_L"]!!, "elbow_L", "trái", "Duỗi tay", "Gập tay")
        check(u["elbow_R"]!!, "elbow_R", "phải", "Duỗi tay", "Gập tay")

        val worst = errors.maxByOrNull { it.first }

        return if (percent > 85) {
            true to "Chuẩn rồi! ($percent%)"
        } else {
            false to (worst?.second ?: "Giữ vững! ($percent%)")
        }
    }

    fun checkPoseAccuracy(landmarks: List<NormalizedLandmark>, poseId: Int): Boolean {
        if (landmarks.isEmpty()) return false

        val rawRef = referenceData?.get(poseId) ?: return false

        val u = mapOf(
            "knee_L" to calculateAngleVector(landmarks[L_HIP], landmarks[L_KNEE], landmarks[L_ANKLE]),
            "knee_R" to calculateAngleVector(landmarks[R_HIP], landmarks[R_KNEE], landmarks[R_ANKLE]),
            "hip_L" to calculateAngleVector(landmarks[L_SHOULDER], landmarks[L_HIP], landmarks[L_KNEE]),
            "hip_R" to calculateAngleVector(landmarks[R_SHOULDER], landmarks[R_HIP], landmarks[R_KNEE]),
            "arm_body_L" to calculateAngleVector(landmarks[L_ELBOW], landmarks[L_SHOULDER], landmarks[L_HIP]),
            "arm_body_R" to calculateAngleVector(landmarks[R_ELBOW], landmarks[R_SHOULDER], landmarks[R_HIP]),
            "elbow_L" to calculateAngleVector(landmarks[L_SHOULDER], landmarks[L_ELBOW], landmarks[L_WRIST]),
            "elbow_R" to calculateAngleVector(landmarks[R_SHOULDER], landmarks[R_ELBOW], landmarks[R_WRIST])
        )

        val swapRef = mapOf(
            "knee_L" to (rawRef["knee_R"] ?: 0.0),
            "knee_R" to (rawRef["knee_L"] ?: 0.0),
            "hip_L" to (rawRef["hip_R"] ?: 0.0),
            "hip_R" to (rawRef["hip_L"] ?: 0.0),
            "arm_body_L" to (rawRef["arm_body_R"] ?: 0.0),
            "arm_body_R" to (rawRef["arm_body_L"] ?: 0.0),
            "elbow_L" to (rawRef["elbow_R"] ?: 0.0),
            "elbow_R" to (rawRef["elbow_L"] ?: 0.0)
        )

        fun calcTotalDiff(refMap: Map<String, Double>): Double {
            return u.entries.sumOf { (k, v) -> angleDiff(v, refMap[k] ?: 0.0) }
        }

        val ref = if (calcTotalDiff(swapRef) < calcTotalDiff(rawRef)) swapRef else rawRef

        val weights = mapOf(
            "knee_L" to 1.0, "knee_R" to 1.0,
            "hip_L" to 1.5, "hip_R" to 1.5,
            "arm_body_L" to 1.2, "arm_body_R" to 1.2,
            "elbow_L" to 0.7, "elbow_R" to 0.7
        )

        var totalScore = 0.0
        var totalWeight = 0.0

        u.forEach { (k, v) ->
            val target = ref[k] ?: return@forEach
            val w = weights[k] ?: 1.0
            val diff = angleDiff(v, target)
            val score = max(0.0, 1 - diff / 90.0)
            totalScore += score * w
            totalWeight += w
        }
        val finalPercent = (totalScore / totalWeight) * 100
        Log.d("YogaCoach", "📸 Static Image Score: ${finalPercent.toInt()}%")
        return finalPercent > 80.0
    }
}