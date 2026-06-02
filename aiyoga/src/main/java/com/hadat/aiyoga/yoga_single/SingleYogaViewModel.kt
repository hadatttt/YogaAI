package com.hadat.aiyoga.yoga_single

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import com.hadat.aiyoga.yoga_ai.YogaPoseModel
import com.hadat.aiyoga.utils.yogautils.YogaCoachUtils
import com.hadat.aiyoga.data.remoteconfig.YogaDataUtils
import hoang.dqm.codebase.base.viewmodel.BaseViewModel
import java.util.*

class SingleYogaViewModel : BaseViewModel() {
    private val _isWaitingForCapture = MutableLiveData(false)
    val isWaitingForCapture: LiveData<Boolean> = _isWaitingForCapture
    private val sessionImagePaths = mutableListOf<String>()
    private var lastFeedback: String = ""
    private var lastFeedbackTime = 0L
    private  val FEEDBACK_HOLD_TIME = 2000L
    private var hasCaptured = false
    private var errorCount = 0
    private var hasStartedCorrectPose = false
    private var isPreviousFrameCorrect = true
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())

    private val timerRunnable = object : Runnable {
        override fun run() {
            if (isCurrentlyCorrect) {
                totalSecondsAccumulated++
                val min = totalSecondsAccumulated / 60
                val sec = totalSecondsAccumulated % 60
                _timerText.value = String.format(Locale.US, "%02d:%02d", min, sec)
            } else {
            }
            handler.postDelayed(this, 1000)
        }
    }
    fun addCapturedImage(path: String) {
        sessionImagePaths.add(path)
    }
    fun getCapturedImages(): List<String> = sessionImagePaths.toList()
    private val _captureTrigger = MutableLiveData<Unit>()
    val captureTrigger: LiveData<Unit> = _captureTrigger

    fun toggleCaptureWait() {
        val isWaiting = !(_isWaitingForCapture.value ?: false)
        _isWaitingForCapture.value = isWaiting

        if (isWaiting) {
            captureStartTime = 0L
            hasCaptured = false
        }
    }
    private val _yogaPoseDataList = MutableLiveData<List<YogaPoseModel>>()

    private val _currentGuideText = MutableLiveData<String>()
    val currentGuideText: LiveData<String> = _currentGuideText

    private val _timerText = MutableLiveData("00:00")
    val timerText: LiveData<String> = _timerText

    private val _isTrackingStarted = MutableLiveData(false)
    val isTrackingStarted: LiveData<Boolean> = _isTrackingStarted

    private val _speakCommand = MutableLiveData<String>()
    val speakCommand: LiveData<String> = _speakCommand

    private var totalSecondsAccumulated = 0
    @Volatile
    private var isCurrentlyCorrect = false
    private var captureStartTime = 0L

    fun fetchYogaPoses() {
        val poses = YogaDataUtils.getAllPoses()
        if (poses.isNotEmpty()) {
            _yogaPoseDataList.value = poses
        }
    }
    fun getErrorCount(): Int {
        return errorCount
    }
    fun startSinglePoseTracking(context: android.content.Context, poseId: Int) {
        resetData()
        _isTrackingStarted.value = true
        _currentGuideText.value = context.getString(com.hadat.aiyoga.R.string.guide_get_ready)
        _speakCommand.value = context.getString(com.hadat.aiyoga.R.string.start_command)
        startLogicalTimer()
    }

    override fun onCleared() {
        resetData()
        super.onCleared()
    }

    fun resetData() {
        handler.removeCallbacks(timerRunnable)
        sessionImagePaths.clear()
        captureStartTime = 0L
        totalSecondsAccumulated = 0
        errorCount = 0
        isCurrentlyCorrect = false
        hasCaptured = false
        hasStartedCorrectPose = false
        isPreviousFrameCorrect = true
        lastFeedback = ""
        lastFeedbackTime = 0L

        _timerText.value = "00:00"
        _currentGuideText.value = ""
        _speakCommand.value = ""
        _isWaitingForCapture.value = false
        _isTrackingStarted.value = false
    }
    fun processCoachLogic(context: android.content.Context, result: PoseLandmarkerResult, poseId: Int) {
        if (poseId == -1 || _isTrackingStarted.value != true) {
            isCurrentlyCorrect = false
            return
        }
        val (isCorrect, feedback) = YogaCoachUtils.getCoachFeedback(context, poseId, result)
        isCurrentlyCorrect = isCorrect
        if (_isWaitingForCapture.value == true && !hasCaptured) {
            if (isCorrect) {
                if (captureStartTime == 0L) {
                    captureStartTime = System.currentTimeMillis()
                }

                val holdTime = System.currentTimeMillis() - captureStartTime
                if (holdTime >= 1500) {
                    hasCaptured = true
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
            errorCount++
        }
        isPreviousFrameCorrect = isCorrect

        val now = System.currentTimeMillis()
        if (now - lastFeedbackTime >= FEEDBACK_HOLD_TIME) {
            val newMessage = if (isCorrect) context.getString(com.hadat.aiyoga.R.string.guide_perfect_counting)
            else "⚠️ $feedback"

            if (newMessage != lastFeedback) {
                lastFeedback = newMessage
                lastFeedbackTime = now
                _currentGuideText.postValue(newMessage)
                if (!isCorrect) _speakCommand.postValue(feedback)
            }
        }
    }
    private fun startLogicalTimer() {
        handler.removeCallbacks(timerRunnable)
        handler.post(timerRunnable)
    }
    fun getTotalTimeStudied(): Int = totalSecondsAccumulated
}
