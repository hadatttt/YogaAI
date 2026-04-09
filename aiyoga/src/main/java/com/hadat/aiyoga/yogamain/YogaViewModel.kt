package com.hadat.aiyoga.yogamain

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import com.hadat.aiyoga.yogautils.YogaCoachUtils
import com.hadat.aiyoga.yogautils.YogaDataUtils
import hoang.dqm.codebase.base.viewmodel.BaseViewModel
import java.util.*

class YogaViewModel : BaseViewModel() {

    private val _yogaPoseDataList = MutableLiveData<List<YogaPoseModel>>()
    val yogaPoseDataList: LiveData<List<YogaPoseModel>> = _yogaPoseDataList

    private val _currentGuideText = MutableLiveData<String>()
    val currentGuideText: LiveData<String> = _currentGuideText

    private val _timerText = MutableLiveData("00:00")
    val timerText: LiveData<String> = _timerText

    private val _isTrackingStarted = MutableLiveData(false)
    val isTrackingStarted: LiveData<Boolean> = _isTrackingStarted

    private val _detectedPoseId = MutableLiveData(-1)
    val detectedPoseId: LiveData<Int> = _detectedPoseId

    private val _previewPoseId = MutableLiveData(-1)
    val previewPoseId: LiveData<Int> = _previewPoseId

    private val _speakCommand = MutableLiveData<String>()
    val speakCommand: LiveData<String> = _speakCommand

    private var lastPoseName: String? = null
    private var poseStartTime: Long = 0
    private var exerciseTimer: Timer? = null
    private var secondsElapsed = 0
    private val PREPARATION_TIME_MS = 2000L

    fun fetchYogaPoses() {
        YogaDataUtils.getRemoteYogaPoses { poses -> poses?.let { _yogaPoseDataList.postValue(it) } }
    }

    fun handlePoseInference(currentPose: String, poseId: Int) {
        if (currentPose == "Unknown" || currentPose == "No Pose") {
            if (lastPoseName != null) {
                resetTracking(null, -1)
                _currentGuideText.postValue("Hãy thực hiện tư thế Yoga")
            }
            return
        }

        // Kiểm tra nếu vẫn là tư thế cũ đang đếm ngược hoặc đang tập
        if (currentPose == lastPoseName) {
            val elapsedTime = System.currentTimeMillis() - poseStartTime

            if (elapsedTime >= PREPARATION_TIME_MS) {
                // ĐÃ HẾT 2 GIÂY CHUẨN BỊ
                if (_isTrackingStarted.value == false) {
                    _isTrackingStarted.postValue(true)
                    startExerciseTimer()
                    _speakCommand.postValue("Bắt đầu")
                }

                // Cập nhật ID để processCoachLogic bắt đầu sửa tư thế
                if (_detectedPoseId.value != poseId) {
                    _detectedPoseId.postValue(poseId)
                }
            } else {
                // ĐANG TRONG 2 GIÂY CHUẨN BỊ -> Hiện đếm ngược
                val countdown = ((PREPARATION_TIME_MS - elapsedTime) / 1000) + 1
                _currentGuideText.postValue("Sẵn sàng: $currentPose ($countdown s)")
            }
        } else {
            // PHÁT HIỆN TƯ THẾ MỚI -> Chỉ Reset 1 lần duy nhất tại đây
            resetTracking(currentPose, poseId)
        }
    }

    fun processCoachLogic(result: PoseLandmarkerResult) {
        val poseId = _detectedPoseId.value ?: -1
        // Chỉ sửa khi ID hợp lệ và cờ tập luyện đã bật (sau 2s chuẩn bị)
        if (poseId != -1 && _isTrackingStarted.value == true) {
            val (isCorrect, feedback) = YogaCoachUtils.getCoachFeedback(poseId, result)
            _currentGuideText.postValue(if (isCorrect) "✅ Tư thế chuẩn!" else "⚠️ $feedback")
            if (!isCorrect) _speakCommand.postValue(feedback)
        }
    }

    private fun resetTracking(poseName: String?, poseId: Int) {
        lastPoseName = poseName
        poseStartTime = System.currentTimeMillis() // Đánh dấu mốc thời gian bắt đầu tư thế mới

        stopExerciseTimer()
        _detectedPoseId.postValue(-1) // Chưa cho phép sửa lỗi
        _previewPoseId.postValue(poseId) // Đổi ảnh mẫu ngay lập tức

        if (poseName != null) {
            _currentGuideText.postValue("Chuẩn bị cho: $poseName")
            _speakCommand.postValue("Chuẩn bị $poseName")
        }
    }

    private fun startExerciseTimer() {
        secondsElapsed = 0
        exerciseTimer?.cancel()
        exerciseTimer = Timer()
        exerciseTimer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                secondsElapsed++
                val min = secondsElapsed / 60
                val sec = secondsElapsed % 60
                _timerText.postValue(String.format("%02d:%02d", min, sec))
            }
        }, 1000, 1000)
    }

    fun stopExerciseTimer() {
        _isTrackingStarted.postValue(false)
        exerciseTimer?.cancel()
        exerciseTimer = null
        _timerText.postValue("00:00")
    }

    override fun onCleared() {
        stopExerciseTimer()
        super.onCleared()
    }
}