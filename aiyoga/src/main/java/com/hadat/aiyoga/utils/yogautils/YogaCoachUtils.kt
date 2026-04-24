package com.hadat.aiyoga.utils.yogautils

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.hadat.aiyoga.R
import com.hadat.aiyoga.data.remoteconfig.YogaDataUtils
import com.hadat.aiyoga.detailyoga.YogaPoseAngleModel
import com.hadat.aiyoga.yogamain.CorrectionRay
import kotlin.math.*

object YogaCoachUtils {

    private var referenceData: Map<Int, YogaPoseAngleModel>? = null
    private val smoothedAngles = mutableMapOf<String, Double>()
    private const val SMOOTH_FACTOR = 0.35f
    private const val ANGLE_THRESHOLD = 25.0
    private var lastScore = 0.0

    private const val L_SHOULDER = 11; private const val R_SHOULDER = 12
    private const val L_ELBOW = 13;    private const val R_ELBOW = 14
    private const val L_WRIST = 15;    private const val R_WRIST = 16
    private const val L_HIP = 23;      private const val R_HIP = 24
    private const val L_KNEE = 25;     private const val R_KNEE = 26
    private const val L_ANKLE = 27;    private const val R_ANKLE = 28

    fun loadReferenceData(onComplete: (Boolean) -> Unit) {
        if (referenceData != null) {
            onComplete(true)
            return
        }

        YogaDataUtils.getRemoteYogaAngles { list ->
            if (list != null) {
                referenceData = list.associateBy { it.id }
                Log.d("YogaCoach", "✅ Loaded ${referenceData?.size} poses from Remote Config")
                onComplete(true)
            } else {
                Log.e("YogaCoach", "❌ Load error: Firebase data is null")
                onComplete(false)
            }
        }
    }
    fun getSeverityLevel(diff: Double): Int {
        return when {
            diff > 50 -> 3
            diff > 30 -> 2
            else -> 1
        }
    }

    fun getCorrectionRays(
        poseId: Int,
        result: PoseLandmarkerResult
    ): List<CorrectionRay> {

        val rays = mutableListOf<CorrectionRay>()
        val drawnBones = mutableSetOf<String>()

        val landmarks = result.landmarks()
        if (landmarks.isNullOrEmpty()) return rays

        val lm = landmarks[0]
        val rawRefModel = referenceData?.get(poseId) ?: return rays

        val u = mapOf(
            "knee_L" to calculateAngleVector(lm[L_HIP], lm[L_KNEE], lm[L_ANKLE]),
            "knee_R" to calculateAngleVector(lm[R_HIP], lm[R_KNEE], lm[R_ANKLE]),
            "hip_L" to calculateAngleVector(lm[L_SHOULDER], lm[L_HIP], lm[L_KNEE]),
            "hip_R" to calculateAngleVector(lm[R_SHOULDER], lm[R_HIP], lm[R_KNEE]),
            "arm_body_L" to calculateAngleVector(lm[L_ELBOW], lm[L_SHOULDER], lm[L_HIP]),
            "arm_body_R" to calculateAngleVector(lm[R_ELBOW], lm[R_SHOULDER], lm[R_HIP]),
            "elbow_L" to calculateAngleVector(lm[L_SHOULDER], lm[L_ELBOW], lm[L_WRIST]),
            "elbow_R" to calculateAngleVector(lm[R_SHOULDER], lm[R_ELBOW], lm[R_WRIST])
        )

        val rawRefMap = getAngleMap(rawRefModel, false)
        val swapRefMap = getAngleMap(rawRefModel, true)

        fun calcTotalDiff(refMap: Map<String, Double>) =
            u.entries.sumOf { angleDiff(it.value, refMap[it.key] ?: 0.0) }

        val ref = if (calcTotalDiff(swapRefMap) < calcTotalDiff(rawRefMap))
            swapRefMap else rawRefMap

        val threshold = ANGLE_THRESHOLD

        // ===== helper
        fun highlightBone(startIdx: Int, endIdx: Int, diff: Double) {
            val id = "$startIdx-$endIdx"
            if (drawnBones.contains(id)) return

            val s = lm[startIdx]
            val e = lm[endIdx]

            rays.add(
                CorrectionRay(
                    s.x(),
                    s.y(),
                    e.x(),
                    e.y(),
                    getSeverityLevel(diff)
                )
            )

            drawnBones.add(id)
        }
        val diffHipL = angleDiff(u["hip_L"]!!, ref["hip_L"]!!)
        if (diffHipL > threshold) highlightBone(L_HIP, L_KNEE, diffHipL)

        val diffHipR = angleDiff(u["hip_R"]!!, ref["hip_R"]!!)
        if (diffHipR > threshold) highlightBone(R_HIP, R_KNEE, diffHipR)

        val diffArmL = angleDiff(u["arm_body_L"]!!, ref["arm_body_L"]!!)
        if (diffArmL > threshold) highlightBone(L_SHOULDER, L_ELBOW, diffArmL)

        val diffArmR = angleDiff(u["arm_body_R"]!!, ref["arm_body_R"]!!)
        if (diffArmR > threshold) highlightBone(R_SHOULDER, R_ELBOW, diffArmR)

        val diffElbowL = angleDiff(u["elbow_L"]!!, ref["elbow_L"]!!)
        if (diffElbowL > threshold) {
            highlightBone(L_SHOULDER, L_ELBOW, diffElbowL)
            highlightBone(L_ELBOW, L_WRIST, diffElbowL)
        }

        val diffElbowR = angleDiff(u["elbow_R"]!!, ref["elbow_R"]!!)
        if (diffElbowR > threshold) {
            highlightBone(R_SHOULDER, R_ELBOW, diffElbowR)
            highlightBone(R_ELBOW, R_WRIST, diffElbowR)
        }

        val diffKneeL = angleDiff(u["knee_L"]!!, ref["knee_L"]!!)
        if (diffKneeL > threshold) {
            highlightBone(L_HIP, L_KNEE, diffKneeL)
            highlightBone(L_KNEE, L_ANKLE, diffKneeL)
        }

        val diffKneeR = angleDiff(u["knee_R"]!!, ref["knee_R"]!!)
        if (diffKneeR > threshold) {
            highlightBone(R_HIP, R_KNEE, diffKneeR)
            highlightBone(R_KNEE, R_ANKLE, diffKneeR)
        }

        return rays
    }
    private fun getAngleMap(model: YogaPoseAngleModel, isSwap: Boolean = false): Map<String, Double> {
        return if (!isSwap) {
            mapOf(
                "knee_L" to model.knee_L, "knee_R" to model.knee_R,
                "hip_L" to model.hip_L, "hip_R" to model.hip_R,
                "arm_body_L" to model.arm_body_L, "arm_body_R" to model.arm_body_R,
                "elbow_L" to model.elbow_L, "elbow_R" to model.elbow_R
            )
        } else {
            mapOf(
                "knee_L" to model.knee_R, "knee_R" to model.knee_L,
                "hip_L" to model.hip_R, "hip_R" to model.hip_L,
                "arm_body_L" to model.arm_body_R, "arm_body_R" to model.arm_body_L,
                "elbow_L" to model.elbow_R, "elbow_R" to model.elbow_L
            )
        }
    }

    private fun normalizeAngle(angle: Double): Double {
        return if (angle > 180) 360 - angle else angle
    }

    private fun angleDiff(a: Double, b: Double): Double {
        val diff = abs(a - b)
        return min(diff, 180 - diff)
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
    private fun isBodyClearlyVisible(lm: List<NormalizedLandmark>): Boolean {
        val criticalPoints = listOf(L_SHOULDER, R_SHOULDER, L_HIP, R_HIP, L_KNEE, R_KNEE, L_ANKLE, R_ANKLE)
        for (idx in criticalPoints) {
            val point = lm[idx]
            if (point.presence().orElse(0f) < 0.5f || point.y() !in 0f..1f || point.x() !in 0f..1f) {
                return false
            }
        }
        return true
    }
    fun getCoachFeedback(context: Context, poseId: Int, result: PoseLandmarkerResult): Pair<Boolean, String> {
        val landmarks = result.landmarks()
        if (landmarks.isNullOrEmpty()) {
            smoothedAngles.clear()
            return false to context.getString(R.string.guide_stand_clear)
        }
        val lm = landmarks[0]

        if (!isBodyClearlyVisible(lm)) {
            return false to context.getString(R.string.stand_back_full_body)
        }
        val rawRefModel = referenceData?.get(poseId) ?: return true to context.getString(R.string.guide_analyzing)

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

        fun calcTotalDiff(a: Map<String, Double>, b: Map<String, Double>): Double {
            return a.entries.sumOf { (k, v) -> angleDiff(v, b[k] ?: 0.0) }
        }

        val rawRefMap = getAngleMap(rawRefModel, isSwap = false)
        val swapRefMap = getAngleMap(rawRefModel, isSwap = true)

        val shouldSwap = calcTotalDiff(u, swapRefMap) < calcTotalDiff(u, rawRefMap)
        val ref = if (shouldSwap) swapRefMap else rawRefMap

        val weights = mapOf(
            "knee_L" to 1.0, "knee_R" to 1.0, "hip_L" to 1.5, "hip_R" to 1.5,
            "arm_body_L" to 1.2, "arm_body_R" to 1.2, "elbow_L" to 0.7, "elbow_R" to 0.7
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

        var finalScore = totalScore / totalWeight
        finalScore = lastScore + 0.3 * (finalScore - lastScore)
        lastScore = finalScore
        val percent = (finalScore * 100).toInt()
        val errors = mutableListOf<Pair<Double, String>>()

        fun check(current: Double, key: String, sideIsLeft: Boolean, lowResId: Int, highResId: Int) {
            val target = ref[key] ?: return
            val diff = angleDiff(current, target)

            if (diff > ANGLE_THRESHOLD) {
                val actualSideIsLeft = if (shouldSwap) !sideIsLeft else sideIsLeft
                val sideStr = context.getString(if (actualSideIsLeft) R.string.side_left else R.string.side_right)
                val actionStr = context.getString(if (current < target) lowResId else highResId)

                errors.add(diff to "$actionStr $sideStr")
            }
        }

        check(u["knee_L"]!!, "knee_L", true, R.string.guide_open_knee, R.string.guide_close_knee)
        check(u["knee_R"]!!, "knee_R", false, R.string.guide_open_knee, R.string.guide_close_knee)
        check(u["hip_L"]!!, "hip_L", true, R.string.guide_lower_hip, R.string.guide_raise_hip)
        check(u["hip_R"]!!, "hip_R", false, R.string.guide_lower_hip, R.string.guide_raise_hip)
        check(u["arm_body_L"]!!, "arm_body_L", true, R.string.guide_raise_arm, R.string.guide_lower_arm)
        check(u["arm_body_R"]!!, "arm_body_R", false, R.string.guide_raise_arm, R.string.guide_lower_arm)
        check(u["elbow_L"]!!, "elbow_L", true, R.string.guide_straighten_arm, R.string.guide_bend_arm)
        check(u["elbow_R"]!!, "elbow_R", false, R.string.guide_straighten_arm, R.string.guide_bend_arm)

        val worst = errors.maxByOrNull { it.first }

        return if (percent > 85) {
            true to "${context.getString(R.string.guide_perfect)} ($percent%)"
        } else {
            val fallback = "${context.getString(R.string.guide_keep_steady)} ($percent%)"
            false to (worst?.second ?: fallback)
        }
    }

    fun checkPoseAccuracy(landmarks: List<NormalizedLandmark>, poseId: Int): Boolean {
        if (landmarks.isEmpty()) return false
        val rawRefModel = referenceData?.get(poseId) ?: return false

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

        val rawRefMap = getAngleMap(rawRefModel, false)
        val swapRefMap = getAngleMap(rawRefModel, true)

        fun calcTotalDiff(refMap: Map<String, Double>): Double {
            return u.entries.sumOf { (k, v) -> angleDiff(v, refMap[k] ?: 0.0) }
        }

        val ref = if (calcTotalDiff(swapRefMap) < calcTotalDiff(rawRefMap)) swapRefMap else rawRefMap

        val weights = mapOf(
            "knee_L" to 1.0, "knee_R" to 1.0, "hip_L" to 1.5, "hip_R" to 1.5,
            "arm_body_L" to 1.2, "arm_body_R" to 1.2, "elbow_L" to 0.7, "elbow_R" to 0.7
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