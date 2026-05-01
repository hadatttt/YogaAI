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
    private  val FEEDBACK_HOLD_TIME = 2000L // 2 giây
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
    @Volatile
    private var isCurrentlyCorrect = false

    fun fetchYogaPoses() {
        val poses = YogaDataUtils.getAllPoses()
        if (poses.isNotEmpty()) {
            _yogaPoseDataList.value = poses
        }
    }

    fun getErrorCount(): Int {
        return errorCount
    }
    fun startSinglePoseTracking( context: android.content.Context,poseId: Int) {
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
    // Trong SingleYogaViewModel.kt

    fun processCoachLogic(context: android.content.Context, result: PoseLandmarkerResult, poseId: Int) {
        if (poseId == -1 || _isTrackingStarted.value != true) {
            isCurrentlyCorrect = false
            return
        }

        val (isCorrect, feedback) = YogaCoachUtils.getCoachFeedback(context, poseId, result)

        // Cập nhật trạng thái bắt đầu để tính errorCount
        if (isCorrect) hasStartedCorrectPose = true
        else if (hasStartedCorrectPose && isPreviousFrameCorrect) errorCount++

        isPreviousFrameCorrect = isCorrect

        val now = System.currentTimeMillis()
        val isHolding = now - lastFeedbackTime < FEEDBACK_HOLD_TIME

        // CHỖ QUAN TRỌNG:
        // Chỉ cập nhật trạng thái logic khi không bị block bởi thời gian giữ feedback
        // Hoặc khi có sự thay đổi rõ rệt về kết quả
        if (!isHolding) {
            val message = if (isCorrect) {
                context.getString(com.hadat.aiyoga.R.string.guide_perfect_counting)
            } else {
                "⚠️ $feedback"
            }

            if (message != lastFeedback) {
                lastFeedback = message
                lastFeedbackTime = now
                _currentGuideText.postValue(message)

                // Timer sẽ nhìn vào biến này để chạy
                isCurrentlyCorrect = isCorrect

                if (!isCorrect) {
                    _speakCommand.postValue(feedback)
                }
            }
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