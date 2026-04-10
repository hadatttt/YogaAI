package com.hadat.aiyoga.singleyoga

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import com.hadat.aiyoga.yogamain.YogaPoseModel
import com.hadat.aiyoga.yogautils.YogaCoachUtils
import com.hadat.aiyoga.yogautils.YogaDataUtils
import hoang.dqm.codebase.base.viewmodel.BaseViewModel
import java.util.*

class SingleYogaViewModel : BaseViewModel() {
    private val _isWaitingForCapture = MutableLiveData(false)
    val isWaitingForCapture: LiveData<Boolean> = _isWaitingForCapture

    private val _captureTrigger = MutableLiveData<Unit>()
    val captureTrigger: LiveData<Unit> = _captureTrigger

    fun toggleCaptureWait() {
        _isWaitingForCapture.value = !(_isWaitingForCapture.value ?: false)
    }
    private val _yogaPoseDataList = MutableLiveData<List<YogaPoseModel>>()
    val yogaPoseDataList: LiveData<List<YogaPoseModel>> = _yogaPoseDataList

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

    fun fetchYogaPoses() {
        YogaDataUtils.getRemoteYogaPoses { poses -> poses?.let { _yogaPoseDataList.postValue(it) } }
    }

    fun startSinglePoseTracking(poseId: Int) {
        exerciseTimer?.cancel()
        exerciseTimer = null
        totalSecondsAccumulated = 0
        isCurrentlyCorrect = false
        _timerText.postValue("00:00")
        _isTrackingStarted.postValue(true)
        startLogicalTimer()
        _currentGuideText.postValue("Vào tư thế để bắt đầu tính giờ!")
        _speakCommand.postValue("Bắt đầu")
    }
    fun stopTracking() {
        exerciseTimer?.cancel()
        exerciseTimer = null
        _isTrackingStarted.postValue(false)
    }
    fun processCoachLogic(result: PoseLandmarkerResult, poseId: Int) {
        if (poseId != -1 && _isTrackingStarted.value == true) {
            val (isCorrect, feedback) = YogaCoachUtils.getCoachFeedback(poseId, result)

            isCurrentlyCorrect = isCorrect
            if (isCorrect && _isWaitingForCapture.value == true) {
                _captureTrigger.postValue(Unit)
                _isWaitingForCapture.postValue(false)
            }
            if (isCorrect) {
                _currentGuideText.postValue("✅ Tư thế chuẩn! Đang đếm giờ...")
            } else {
                _currentGuideText.postValue("⚠️ $feedback")
                _speakCommand.postValue(feedback)
            }
        } else {
            isCurrentlyCorrect = false
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
}