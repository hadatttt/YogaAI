package com.hadat.aiyoga.yogautils

import android.content.Context
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.math.*

object YogaCoachUtils {

    private var referenceData: Map<Int, Map<String, Double>>? = null

    private const val L_SHOULDER = 11; private const val R_SHOULDER = 12
    private const val L_ELBOW = 13;    private const val R_ELBOW = 14
    private const val L_WRIST = 15;    private const val R_WRIST = 16
    private const val L_HIP = 23;      private const val R_HIP = 24
    private const val L_KNEE = 25;     private const val R_KNEE = 26
    private const val L_ANKLE = 27;    private const val R_ANKLE = 28

    fun loadReferenceData(context: Context) {
        val data = mutableMapOf<Int, Map<String, Double>>()
        try {
            val inputStream = context.assets.open("yoga_golden_v3.csv")
            val reader = BufferedReader(InputStreamReader(inputStream))
            val header = reader.readLine()?.split(",") ?: return

            reader.forEachLine { line ->
                val tokens = line.split(",")
                if (tokens.size >= header.size) {
                    val poseId = tokens[0].toDoubleOrNull()?.toInt() ?: return@forEachLine
                    val values = mutableMapOf<String, Double>()
                    for (i in 1 until tokens.size) {
                        values[header[i].trim()] = tokens[i].toDoubleOrNull() ?: 0.0
                    }
                    data[poseId] = values
                }
            }
            referenceData = data
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun normalizeLandmarks(landmarks: List<NormalizedLandmark>): List<Pair<Double, Double>> {
        val lH = landmarks[L_HIP]
        val rH = landmarks[R_HIP]
        val lS = landmarks[L_SHOULDER]

        val centerX = (lH.x() + rH.x()) / 2.0
        val centerY = (lH.y() + rH.y()) / 2.0
        val scale = sqrt((lS.x() - centerX).pow(2) + (lS.y() - centerY).pow(2)) + 1e-6

        return landmarks.map { Pair((it.x() - centerX).toDouble() / scale, (it.y() - centerY).toDouble() / scale) }
    }

    private fun angle(p1: Pair<Double, Double>, p2: Pair<Double, Double>, p3: Pair<Double, Double>): Double {
        val baX = p1.first - p2.first
        val baY = p1.second - p2.second
        val bcX = p3.first - p2.first
        val bcY = p3.second - p2.second
        val dot = baX * bcX + baY * bcY
        val magBA = sqrt(baX * baX + baY * baY)
        val magBC = sqrt(bcX * bcX + bcY * bcY)
        val cos = dot / (magBA * magBC + 1e-6)
        return acos(cos.coerceIn(-1.0, 1.0)) * 180.0 / PI
    }

    private fun isVisible(lm: NormalizedLandmark?): Boolean {
        return lm != null && (lm.visibility().orElse(0.0f) > 0.5f)
    }

    fun getCoachFeedback(poseId: Int, result: PoseLandmarkerResult): Pair<Boolean, String> {
        val allLandmarks = result.landmarks()
        if (allLandmarks.isNullOrEmpty()) return false to "Không tìm thấy người"

        val landmarks = allLandmarks[0]
        val rawRef = referenceData?.get(poseId) ?: return true to "Đang tải dữ liệu..."
        if (landmarks.size < 29) return false to "Dữ liệu không đủ"

        // Kiểm tra các điểm quan trọng để normalize
        if (!isVisible(landmarks[L_SHOULDER]) || !isVisible(landmarks[R_SHOULDER]) ||
            !isVisible(landmarks[L_HIP]) || !isVisible(landmarks[R_HIP])) {
            return false to "Hãy đứng rõ vào khung hình"
        }

        val norm = normalizeLandmarks(landmarks)
        fun N(i: Int) = norm[i]

        val uKneeL = if (isVisible(landmarks[L_ANKLE])) angle(N(L_HIP), N(L_KNEE), N(L_ANKLE)) else 180.0
        val uKneeR = if (isVisible(landmarks[R_ANKLE])) angle(N(R_HIP), N(R_KNEE), N(R_ANKLE)) else 180.0
        val uArmBodyL = if (isVisible(landmarks[L_ELBOW])) angle(N(L_ELBOW), N(L_SHOULDER), N(L_HIP)) else 90.0
        val uArmBodyR = if (isVisible(landmarks[R_ELBOW])) angle(N(R_ELBOW), N(R_SHOULDER), N(R_HIP)) else 90.0

        val refKneeL = rawRef["knee_L"] ?: 180.0
        val refKneeR = rawRef["knee_R"] ?: 180.0
        val refArmL = rawRef["arm_body_L"] ?: 90.0
        val refArmR = rawRef["arm_body_R"] ?: 90.0

        val diffNormal = abs(uKneeL - refKneeL) + abs(uKneeR - refKneeR) + abs(uArmBodyL - refArmL) + abs(uArmBodyR - refArmR)
        val diffSwap = abs(uKneeL - refKneeR) + abs(uKneeR - refKneeL) + abs(uArmBodyL - refArmR) + abs(uArmBodyR - refArmL)

        val shouldSwap = diffSwap < diffNormal - 30.0

        val ref = if (shouldSwap) {
            mapOf(
                "knee_L" to refKneeR, "knee_R" to refKneeL,
                "hip_L" to (rawRef["hip_R"] ?: 180.0), "hip_R" to (rawRef["hip_L"] ?: 180.0),
                "arm_body_L" to refArmR, "arm_body_R" to refArmL,
                "elbow_L" to (rawRef["elbow_R"] ?: 180.0), "elbow_R" to (rawRef["elbow_L"] ?: 180.0)
            )
        } else rawRef

        // --- KIỂM TRA SAI SỐ ---
        val tolerance = if (poseId == 17 || poseId == 70) 35.0 else 25.0
        val feedback = mutableListOf<String>()

        fun check(current: Double, key: String, low: String, high: String) {
            val target = ref[key] ?: return
            if (target > 1.0 && abs(current - target) > tolerance) {
                feedback.add(if (current < target) low else high)
            }
        }

        if (isVisible(landmarks[L_ANKLE])) check(uKneeL, "knee_L", "Mở gối trái", "Khép gối trái")
        if (isVisible(landmarks[R_ANKLE])) check(uKneeR, "knee_R", "Mở gối phải", "Khép gối phải")

        check(angle(N(L_SHOULDER), N(L_HIP), N(L_KNEE)), "hip_L", "Mở hông trái", "Khép hông trái")
        check(angle(N(R_SHOULDER), N(R_HIP), N(R_KNEE)), "hip_R", "Mở hông phải", "Khép hông phải")

        check(uArmBodyL, "arm_body_L", "Nâng tay trái", "Hạ tay trái")
        check(uArmBodyR, "arm_body_R", "Nâng tay phải", "Hạ tay phải")

        if (isVisible(landmarks[L_WRIST])) check(angle(N(L_SHOULDER), N(L_ELBOW), N(L_WRIST)), "elbow_L", "Duỗi tay trái", "Gập tay trái")
        if (isVisible(landmarks[R_WRIST])) check(angle(N(R_SHOULDER), N(R_ELBOW), N(R_WRIST)), "elbow_R", "Duỗi tay phải", "Gập tay phải")

        return if (feedback.isEmpty()) true to "Tư thế chuẩn, giữ nguyên!"
        else false to feedback.first()
    }
}