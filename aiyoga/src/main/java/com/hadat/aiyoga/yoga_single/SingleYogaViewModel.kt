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

    private var errorCount = 0
    private var hasStartedCorrectPose = false
    private var isPreviousFrameCorrect = true

    fun addCapturedImage(path: String) {
        sessionImagePaths.add(path)
    }
    fun getCapturedImages(): List<String> = sessionImagePaths.toList()
    private val _captureTrigger = MutableLiveData<Unit>()
    val captureTrigger: LiveData<Unit> = _captureTrigger

    fun toggleCaptureWait() {
        _isWaitingForCapture.value = !(_isWaitingForCapture.value ?: false)
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

    private var exerciseTimer: Timer? = null
    private var totalSecondsAccumulated = 0
    private var isCurrentlyCorrect = false

    fun fetchYogaPoses(context: android.content.Context) {
        YogaDataUtils.getRemoteYogaPoses(context.applicationContext) { poses -> poses?.let { _yogaPoseDataList.postValue(it) } }
    }

    fun getErrorCount(): Int {
        return errorCount
    }
    fun startSinglePoseTracking(context: android.content.Context, poseId: Int) {
        exerciseTimer?.cancel()
        exerciseTimer = null
        totalSecondsAccumulated = 0
        isCurrentlyCorrect = false
        errorCount = 0
        hasStartedCorrectPose = false
        isPreviousFrameCorrect = true
        sessionImagePaths.clear()
        _timerText.postValue("00:00")
        _isTrackingStarted.postValue(true)
        startLogicalTimer()
        _currentGuideText.postValue(context.getString(com.hadat.aiyoga.R.string.guide_get_ready))
        _speakCommand.postValue(context.getString(com.hadat.aiyoga.R.string.start_command))
    }
    fun stopTracking() {
        exerciseTimer?.cancel()
        exerciseTimer = null
        _isTrackingStarted.postValue(false)
    }
    fun processCoachLogic(context: android.content.Context, result: PoseLandmarkerResult, poseId: Int) {
        if (poseId != -1 && _isTrackingStarted.value == true) {
            val (isCorrect, feedback) = YogaCoachUtils.getCoachFeedback(context, poseId, result)

            if (isCorrect) {
                hasStartedCorrectPose = true
            } else {
                if (hasStartedCorrectPose && isPreviousFrameCorrect) {
                    errorCount++
                }
            }

            isPreviousFrameCorrect = isCorrect
            isCurrentlyCorrect = isCorrect
            if (isCorrect && _isWaitingForCapture.value == true) {
                _captureTrigger.postValue(Unit)
                _isWaitingForCapture.postValue(false)
            }

            if (isCorrect) {
                val perfectMsg = context.getString(com.hadat.aiyoga.R.string.guide_perfect_counting)
                _currentGuideText.postValue(perfectMsg)
            } else {
                _currentGuideText.postValue("⚠️ $feedback")
                _speakCommand.postValue(feedback)
            }
        } else {
            isCurrentlyCorrect = false
            isPreviousFrameCorrect = true
        }
    }

    private fun startLogicalTimer() {
        exerciseTimer?.cancel()
        exerciseTimer = Timer()
        exerciseTimer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                if (isCurrentlyCorrect) {
                    totalSecondsAccumulated++
                    updateTimerUI()
                }
            }
        }, 1000, 1000)
    }

    private fun updateTimerUI() {
        val min = totalSecondsAccumulated / 60
        val sec = totalSecondsAccumulated % 60
        _timerText.postValue(String.format("%02d:%02d", min, sec))
    }

    fun getTotalTimeStudied(): Int = totalSecondsAccumulated

    override fun onCleared() {
        exerciseTimer?.cancel()
        super.onCleared()
    }
    fun resetData() {
        exerciseTimer?.cancel()
        exerciseTimer = null
        totalSecondsAccumulated = 0
        errorCount = 0
        isCurrentlyCorrect = false
        hasStartedCorrectPose = false
        isPreviousFrameCorrect = true
        sessionImagePaths.clear()
        _timerText.value = "00:00"
        _currentGuideText.value = ""
        _isTrackingStarted.value = false
        _isWaitingForCapture.value = false
    }
}