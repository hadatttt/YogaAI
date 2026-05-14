package com.hadat.aiyoga.utils.yogautils

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import com.google.gson.GsonBuilder
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.hadat.aiyoga.data.remoteconfig.YogaDataUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.acos
import kotlin.math.round
import kotlin.math.sqrt

object YogaAssetAngleScanner {
    private const val TAG = "YogaAssetAngleScanner"
    private const val ASSET_DIR = "yogadata"
    private const val FIRST_POSE_ID = 0
    private const val LAST_POSE_ID = 81
    private const val LOG_CHUNK_SIZE = 3500

    private const val L_SHOULDER = 11
    private const val R_SHOULDER = 12
    private const val L_ELBOW = 13
    private const val R_ELBOW = 14
    private const val L_WRIST = 15
    private const val R_WRIST = 16
    private const val L_HIP = 23
    private const val R_HIP = 24
    private const val L_KNEE = 25
    private const val R_KNEE = 26
    private const val L_ANKLE = 27
    private const val R_ANKLE = 28

    fun scanBaseAssetsAndLog(context: Context) {
        val appContext = context.applicationContext

        CoroutineScope(Dispatchers.Default).launch {
            val helper = PoseLandmarkerHelper(
                context = appContext,
                runningMode = RunningMode.IMAGE,
                currentModel = PoseLandmarkerHelper.MODEL_POSE_LANDMARKER_FULL,
                currentDelegate = PoseLandmarkerHelper.DELEGATE_CPU,
                poseLandmarkerHelperListener = object : PoseLandmarkerHelper.LandmarkerListener {
                    override fun onError(error: String, errorCode: Int) {
                        Log.e(TAG, error)
                    }

                    override fun onResults(resultBundle: PoseLandmarkerHelper.ResultBundle) = Unit
                }
            )

            val scannedAngles = mutableListOf<GoldenPoseAngle>()

            for (id in FIRST_POSE_ID..LAST_POSE_ID) {
                val assetPath = "$ASSET_DIR/$id.webp"
                try {
                    appContext.assets.open(assetPath).use { inputStream ->
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        val result = helper.detectImage(bitmap)?.results?.firstOrNull()
                        bitmap.recycle()

                        val landmarks = result?.landmarks()?.firstOrNull()
                        if (landmarks == null) {
                            Log.w(TAG, "No pose landmarks detected for asset: $assetPath")
                            return@use
                        }

                        scannedAngles.add(buildGoldenPoseAngle(id, landmarks))
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to scan asset: $assetPath", e)
                }
            }

            helper.clearPoseLandmarker()
            logJson(scannedAngles)
        }
    }

    private fun buildGoldenPoseAngle(
        id: Int,
        lm: List<NormalizedLandmark>
    ): GoldenPoseAngle {
        return GoldenPoseAngle(
            id = id,
            name = YogaDataUtils.getPoseById(id)?.name ?: "Pose $id",
            knee_L = angle(lm[L_HIP], lm[L_KNEE], lm[L_ANKLE]).roundOneDecimal(),
            knee_R = angle(lm[R_HIP], lm[R_KNEE], lm[R_ANKLE]).roundOneDecimal(),
            hip_L = angle(lm[L_SHOULDER], lm[L_HIP], lm[L_KNEE]).roundOneDecimal(),
            hip_R = angle(lm[R_SHOULDER], lm[R_HIP], lm[R_KNEE]).roundOneDecimal(),
            arm_body_L = angle(lm[L_ELBOW], lm[L_SHOULDER], lm[L_HIP]).roundOneDecimal(),
            arm_body_R = angle(lm[R_ELBOW], lm[R_SHOULDER], lm[R_HIP]).roundOneDecimal(),
            elbow_L = angle(lm[L_SHOULDER], lm[L_ELBOW], lm[L_WRIST]).roundOneDecimal(),
            elbow_R = angle(lm[R_SHOULDER], lm[R_ELBOW], lm[R_WRIST]).roundOneDecimal()
        )
    }

    private fun angle(
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
        val degrees = Math.toDegrees(acos(cos.toDouble()))
        return if (degrees > 180) 360 - degrees else degrees
    }

    private fun Double.roundOneDecimal(): Double = round(this * 10.0) / 10.0

    private fun logJson(scannedAngles: List<GoldenPoseAngle>) {
        val json = GsonBuilder().setPrettyPrinting().create().toJson(scannedAngles)
        Log.d(TAG, "Scanned ${scannedAngles.size} yoga asset poses")
        json.chunked(LOG_CHUNK_SIZE).forEachIndexed { index, chunk ->
            Log.d(TAG, "golden_angles_json_part_${index + 1}: $chunk")
        }
    }

    data class GoldenPoseAngle(
        val id: Int,
        val name: String,
        val knee_L: Double,
        val knee_R: Double,
        val hip_L: Double,
        val hip_R: Double,
        val arm_body_L: Double,
        val arm_body_R: Double,
        val elbow_L: Double,
        val elbow_R: Double
    )
}
