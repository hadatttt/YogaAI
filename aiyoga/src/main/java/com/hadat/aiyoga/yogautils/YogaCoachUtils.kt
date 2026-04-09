package com.hadat.aiyoga.yogautils

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale
import kotlin.math.*

object YogaCoachUtils {

    private var referenceData: Map<Int, Map<String, Double>>? = null
    private var poseLabels = mutableMapOf<Int, String>()

    private val smoothedAngles = mutableMapOf<String, Double>()
    private const val SMOOTH_FACTOR = 0.35f

    // --- HẰNG SỐ LANDMARK INDEX (Theo chuẩn MediaPipe) ---
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
            val inputStream = context.assets.open("yoga_golden_v3.csv")
            val reader = BufferedReader(InputStreamReader(inputStream))
            val headerLine = reader.readLine() ?: return
            val header = headerLine.split(",").map { it.trim() }

            reader.forEachLine { line ->
                val tokens = line.split(",")
                if (tokens.size >= header.size) {
                    val poseId = tokens[0].toDoubleOrNull()?.toInt() ?: return@forEachLine
                    labels[poseId] = tokens[1].trim()

                    val values = mutableMapOf<String, Double>()
                    for (i in 2 until tokens.size) {
                        values[header[i]] = tokens[i].toDoubleOrNull() ?: 0.0
                    }
                    data[poseId] = values
                }
            }
            referenceData = data
            poseLabels = labels
            Log.d("YogaCoach", "✅ Loaded ${data.size} poses from CSV")
        } catch (e: Exception) {
            Log.e("YogaCoach", "❌ Load Error: ${e.message}")
        }
    }

    fun getPoseLabels(): List<String> {
        if (poseLabels.isEmpty()) return emptyList()
        return poseLabels.keys.sorted().map { poseLabels[it] ?: "Unknown" }
    }

    /**
     * TÍNH GÓC BẰNG VECTOR (DOT PRODUCT):
     * Phương pháp chuẩn để triệt tiêu sai số xoay ảnh và tỉ lệ khung hình.
     */
    private fun calculateAngleVector(a: NormalizedLandmark, b: NormalizedLandmark, c: NormalizedLandmark): Double {
        val v1x = a.x().toDouble() - b.x().toDouble()
        val v1y = a.y().toDouble() - b.y().toDouble()

        val v2x = c.x().toDouble() - b.x().toDouble()
        val v2y = c.y().toDouble() - b.y().toDouble()

        val dotProduct = v1x * v2x + v1y * v2y
        val mag1 = sqrt(v1x * v1x + v1y * v1y)
        val mag2 = sqrt(v2x * v2x + v2y * v2y)

        if (mag1 == 0.0 || mag2 == 0.0) return 0.0

        val cosTheta = dotProduct / (mag1 * mag2)
        val angleRad = acos(cosTheta.coerceIn(-1.0, 1.0))

        return Math.toDegrees(angleRad)
    }

    /**
     * HÀM DÙNG CHO BATCH EXTRACTION:
     * Trích xuất toàn bộ 8 góc từ một bộ Landmark của ảnh tĩnh.
     */
    fun extractAllAnglesFromLandmarks(lm: List<NormalizedLandmark>): Map<String, Double> {
        return mapOf(
            "knee_L" to calculateAngleVector(lm[L_HIP], lm[L_KNEE], lm[L_ANKLE]),
            "knee_R" to calculateAngleVector(lm[R_HIP], lm[R_KNEE], lm[R_ANKLE]),
            "hip_L" to calculateAngleVector(lm[L_SHOULDER], lm[L_HIP], lm[L_KNEE]),
            "hip_R" to calculateAngleVector(lm[R_SHOULDER], lm[R_HIP], lm[R_KNEE]),
            "arm_body_L" to calculateAngleVector(lm[L_ELBOW], lm[L_SHOULDER], lm[L_HIP]),
            "arm_body_R" to calculateAngleVector(lm[R_ELBOW], lm[R_SHOULDER], lm[R_HIP]),
            "elbow_L" to calculateAngleVector(lm[L_SHOULDER], lm[L_ELBOW], lm[L_WRIST]),
            "elbow_R" to calculateAngleVector(lm[R_SHOULDER], lm[R_ELBOW], lm[R_WRIST])
        )
    }

    // Giữ lại hàm Static cũ nếu cần gọi lẻ lẻ, nhưng nên dùng hàm extractAllAngles ở trên
    fun calculateAngleStatic(p1: NormalizedLandmark, p2: NormalizedLandmark, p3: NormalizedLandmark): Double {
        return calculateAngleVector(p1, p2, p3)
    }

    private fun getSmoothAngle(key: String, newAngle: Double): Double {
        val oldAngle = smoothedAngles[key] ?: newAngle
        val smoothed = oldAngle + (SMOOTH_FACTOR * (newAngle - oldAngle))
        smoothedAngles[key] = smoothed
        return smoothed
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

        // 1. Tính toán & Làm mượt 8 góc thực tế bằng Vector
        val uKneeL = getSmoothAngle("knee_L", calculateAngleVector(lm[L_HIP], lm[L_KNEE], lm[L_ANKLE]))
        val uKneeR = getSmoothAngle("knee_R", calculateAngleVector(lm[R_HIP], lm[R_KNEE], lm[R_ANKLE]))
        val uHipL  = getSmoothAngle("hip_L", calculateAngleVector(lm[L_SHOULDER], lm[L_HIP], lm[L_KNEE]))
        val uHipR  = getSmoothAngle("hip_R", calculateAngleVector(lm[R_SHOULDER], lm[R_HIP], lm[R_KNEE]))
        val uArmL  = getSmoothAngle("arm_body_L", calculateAngleVector(lm[L_ELBOW], lm[L_SHOULDER], lm[L_HIP]))
        val uArmR  = getSmoothAngle("arm_body_R", calculateAngleVector(lm[R_ELBOW], lm[R_SHOULDER], lm[R_HIP]))
        val uElbowL = getSmoothAngle("elbow_L", calculateAngleVector(lm[L_SHOULDER], lm[L_ELBOW], lm[L_WRIST]))
        val uElbowR = getSmoothAngle("elbow_R", calculateAngleVector(lm[R_SHOULDER], lm[R_ELBOW], lm[R_WRIST]))

        // 2. Logic SMART SWAP (Hỗ trợ lật gương camera trước)
        val rKL = rawRef["knee_L"] ?: 180.0
        val rKR = rawRef["knee_R"] ?: 180.0
        val diffNormal = abs(uKneeL - rKL) + abs(uKneeR - rKR)
        val diffSwap = abs(uKneeL - rKR) + abs(uKneeR - rKL)
        val shouldSwap = diffSwap < (diffNormal - 40.0)

        val ref = if (shouldSwap) {
            mapOf(
                "knee_L" to rKR, "knee_R" to rKL,
                "hip_L" to (rawRef["hip_R"] ?: 180.0), "hip_R" to (rawRef["hip_L"] ?: 180.0),
                "arm_body_L" to (rawRef["arm_body_R"] ?: 90.0), "arm_body_R" to (rawRef["arm_body_L"] ?: 90.0),
                "elbow_L" to (rawRef["elbow_R"] ?: 180.0), "elbow_R" to (rawRef["elbow_L"] ?: 180.0)
            )
        } else rawRef

        // 3. LOG CHECK GÓC (Đối chiếu với CSV)
        Log.d("YogaCoach", "--- [$poseName] (Vector Mode) ---")
        val currentAngles = mapOf(
            "knee_L" to uKneeL, "knee_R" to uKneeR,
            "hip_L" to uHipL, "hip_R" to uHipR,
            "arm_body_L" to uArmL, "arm_body_R" to uArmR,
            "elbow_L" to uElbowL, "elbow_R" to uElbowR
        )
        currentAngles.forEach { (key, current) ->
            val target = ref[key] ?: 0.0
            Log.d("YogaCoach", "$key: Máy đo: ${"%.1f".format(current)}° | Mẫu CSV: ${"%.1f".format(target)}°")
        }

        // 4. KIỂM TRA SAI SỐ
        val errors = mutableListOf<Pair<Double, String>>()
        val tolerance = 25.0

        fun check(current: Double, key: String, side: String, low: String, high: String) {
            val target = ref[key] ?: return
            if (target > 1.0 && abs(current - target) > tolerance) {
                val displaySide = if (shouldSwap) (if (side == "trái") "phải" else "trái") else side
                val msg = if (current < target) low else high
                errors.add(abs(current - target) to "$msg $displaySide")
            }
        }

        check(uKneeL, "knee_L", "trái", "Mở gối", "Khép gối")
        check(uKneeR, "knee_R", "phải", "Mở gối", "Khép gối")
        check(uHipL, "hip_L", "trái", "Hạ hông", "Nâng hông")
        check(uHipR, "hip_R", "phải", "Hạ hông", "Nâng hông")
        check(uArmL, "arm_body_L", "trái", "Nâng tay", "Hạ tay")
        check(uArmR, "arm_body_R", "phải", "Nâng tay", "Hạ tay")
        check(uElbowL, "elbow_L", "trái", "Duỗi tay", "Gập tay")
        check(uElbowR, "elbow_R", "phải", "Duỗi tay", "Gập tay")

        val worst = errors.maxByOrNull { it.first }
        return if (worst == null) true to "Giữ vững tư thế!"
        else false to worst.second
    }
}