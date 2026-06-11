package com.hadat.aiyoga.yoga_ai

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import com.hadat.aiyoga.R
import com.hadat.aiyoga.utils.service.AppPreferences
import com.hadat.aiyoga.utils.yogautils.YogaCoachUtils
import com.hadat.aiyoga.data.remoteconfig.YogaDataUtils
import com.hadat.aiyoga.yoga_single.WorkoutResultModel
import hoang.dqm.codebase.base.viewmodel.BaseViewModel
import java.text.SimpleDateFormat
import java.util.*

class YogaViewModel : BaseViewModel() {
    private val _isWaitingForCapture = MutableLiveData(false)
    val isWaitingForCapture: LiveData<Boolean> = _isWaitingForCapture

    private val _captureTrigger = MutableLiveData<Unit>()
    val captureTrigger: LiveData<Unit> = _captureTrigger

    private val sessionImagePaths = mutableListOf<String>()
    private val workoutSessionTracker = mutableMapOf<Int, WorkoutResultModel>()

    private val _yogaPoseDataList = MutableLiveData<List<YogaPoseModel>>()
    val yogaPoseDataList: LiveData<List<YogaPoseModel>> = _yogaPoseDataList

    private val _currentPoseName = MutableLiveData("...")
    val currentPoseName: LiveData<String> = _currentPoseName

    private val _currentGuideText = MutableLiveData<String>()
    val currentGuideText: LiveData<String> = _currentGuideText

    private val _timerText = MutableLiveData("00:00")
    val timerText: LiveData<String> = _timerText

    private val _detectedPoseId = MutableLiveData(-1)

    private val _previewPoseId = MutableLiveData(-1)
    val previewPoseId: LiveData<Int> = _previewPoseId

    private val _speakCommand = MutableLiveData<String>()
    val speakCommand: LiveData<String> = _speakCommand

    private var lastPoseName: String? = null
    private var poseStartTime: Long = 0
    private var isTrackingStarted = false

    private val handler = Handler(Looper.getMainLooper())
    private val timerRunnable = object : Runnable {
        override fun run() {
            if (isCurrentlyCorrect && isTrackingStarted) {
                val currentId = _detectedPoseId.value ?: -1
                if (currentId != -1) {
                    workoutSessionTracker[currentId]?.let { result ->
                        result.durationInSeconds++
                        val min = result.durationInSeconds / 60
                        val sec = result.durationInSeconds % 60
                        _timerText.value = String.format(Locale.US, "%02d:%02d", min, sec)
                    }
                }
            }
            handler.postDelayed(this, 1000)
        }
    }

    @Volatile
    private var isCurrentlyCorrect = false
    private val PREPARATION_TIME_MS = 3000L

    private var hasStartedCorrectPose = false
    private var isPreviousFrameCorrect = true
    private var hasCapturedCurrentPose = false
    private var captureStartTime = 0L

    private var lastFeedback = ""
    private var lastFeedbackTime = 0L
    private val FEEDBACK_HOLD_TIME = 2000L

    fun toggleCaptureWait() {
        _isWaitingForCapture.value = !(_isWaitingForCapture.value ?: false)
        if (_isWaitingForCapture.value == true) {
            captureStartTime = 0L
            hasCapturedCurrentPose = false
        }
    }

    fun addCapturedImage(path: String) = sessionImagePaths.add(path)
    fun getCapturedImages(): List<String> = sessionImagePaths.toList()

    fun processCoachLogic(context: Context, result: PoseLandmarkerResult) {
        val poseId = _detectedPoseId.value ?: -1
        if (poseId == -1 || !isTrackingStarted) {
            isCurrentlyCorrect = false
            return
        }

        val (isCorrect, feedback) = YogaCoachUtils.getCoachFeedback(context, poseId, result)
        isCurrentlyCorrect = isCorrect

        val currentResult = workoutSessionTracker.getOrPut(poseId) {
            WorkoutResultModel(
                userId = AppPreferences.getUserId(context) ?: "guest",
                poseId = poseId,
                poseName = _currentPoseName.value ?: "Unknown",
                poseUrl = _yogaPoseDataList.value?.find { it.id == poseId }?.photo_url ?: "",
                durationInSeconds = 0,
                date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()),
                errorCount = 0,
                workoutTimestamp = System.currentTimeMillis()
            )
        }

        if (_isWaitingForCapture.value == true && !hasCapturedCurrentPose) {
            if (isCorrect) {
                if (captureStartTime == 0L) captureStartTime = System.currentTimeMillis()
                if (System.currentTimeMillis() - captureStartTime >= 1500) {
                    hasCapturedCurrentPose = true
                    _captureTrigger.postValue(Unit)
                    _isWaitingForCapture.postValue(false)
                    captureStartTime = 0L
                }
            } else {
                captureStartTime = 0L
            }
        }

        if (isCorrect) {
            hasStartedCorrectPose = true
        } else if (hasStartedCorrectPose && isPreviousFrameCorrect) {
            currentResult.errorCount++
        }
        isPreviousFrameCorrect = isCorrect

        val now = System.currentTimeMillis()
        if (now - lastFeedbackTime >= FEEDBACK_HOLD_TIME) {
            val newMessage = if (isCorrect) context.getString(R.string.guide_perfect_counting)
            else "⚠️ $feedback"

            if (newMessage != lastFeedback) {
                lastFeedback = newMessage
                lastFeedbackTime = now
                _currentGuideText.postValue(newMessage)
                if (!isCorrect) _speakCommand.postValue(feedback)
            }
        }
    }

    fun showBodyNotReadyGuide(context: Context) {
        isCurrentlyCorrect = false
        _currentGuideText.postValue(context.getString(R.string.stand_back_full_body))
    }

    fun handlePoseInference(context: Context, poseId: Int) {
        val allPoses = _yogaPoseDataList.value ?: return
        val poseData = allPoses.find { it.id == poseId } ?: return
        val currentPose = poseData.name

        if (currentPose == lastPoseName) {
            val elapsedTime = System.currentTimeMillis() - poseStartTime

            if (elapsedTime >= PREPARATION_TIME_MS) {
                if (!isTrackingStarted) {
                    isTrackingStarted = true
                    hasStartedCorrectPose = false
                    isPreviousFrameCorrect = true
                    hasCapturedCurrentPose = false
                    captureStartTime = 0L

                    startLogicalTimer()

                    _previewPoseId.postValue(poseId)
                    _currentPoseName.postValue(currentPose)
                    _speakCommand.postValue("${context.getString(R.string.start_practicing)} $currentPose")
                }
                _detectedPoseId.postValue(poseId)
            } else {
                val countdown = 3 - (elapsedTime / 1000)
                _currentGuideText.postValue("${context.getString(R.string.hold_pose)} $currentPose ($countdown s)")
            }
        } else {
            lastPoseName = currentPose
            poseStartTime = System.currentTimeMillis()
            _detectedPoseId.postValue(-1)
            _previewPoseId.postValue(-1)
            isCurrentlyCorrect = false
            if (isTrackingStarted) stopExerciseTimer()
            _currentGuideText.postValue("${context.getString(R.string.prepare_pose)}: $currentPose")
        }
    }

    private fun startLogicalTimer() {
        handler.removeCallbacks(timerRunnable)
        handler.post(timerRunnable)
    }

    fun stopExerciseTimer() {
        handler.removeCallbacks(timerRunnable)
        isTrackingStarted = false
        isCurrentlyCorrect = false
        _timerText.postValue("00:00")
        _detectedPoseId.postValue(-1)
    }

    fun fetchYogaPoses() {
        val poses = YogaDataUtils.getAllPoses()
        if (poses.isNotEmpty()) _yogaPoseDataList.value = poses
    }

    fun getFinalWorkoutResults(): Array<WorkoutResultModel> {
        return workoutSessionTracker.values
            .filter { it.durationInSeconds > 0 }
            .toTypedArray()
    }

    fun clearData() {
        workoutSessionTracker.clear()
        sessionImagePaths.clear()
        lastPoseName = null
        stopExerciseTimer()
    }


    fun resetData() {
        stopExerciseTimer()

        workoutSessionTracker.clear()
        sessionImagePaths.clear()
        lastPoseName = null
        poseStartTime = 0

        _currentPoseName.value = "..."
        _currentGuideText.value = ""
        _speakCommand.value = ""
        _timerText.value = "00:00"
        _detectedPoseId.value = -1
        _previewPoseId.value = -1
        _isWaitingForCapture.value = false

        isTrackingStarted = false
        isCurrentlyCorrect = false
        hasCapturedCurrentPose = false
        hasStartedCorrectPose = false
        isPreviousFrameCorrect = true
        lastFeedback = ""
        lastFeedbackTime = 0L
        captureStartTime = 0L
    }

    override fun onCleared() {
        resetData()
        super.onCleared()
    }
}
