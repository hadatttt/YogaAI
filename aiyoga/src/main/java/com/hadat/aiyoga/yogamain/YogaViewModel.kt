package com.hadat.aiyoga.yogamain

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import com.hadat.aiyoga.sequence.SequenceModel
import com.hadat.aiyoga.utils.yogautils.YogaCoachUtils
import com.hadat.aiyoga.utils.yogautils.YogaDataUtils
import hoang.dqm.codebase.base.viewmodel.BaseViewModel
import java.util.*
import kotlin.collections.ArrayList

class YogaViewModel : BaseViewModel() {
    private val _isWaitingForCapture = MutableLiveData(false)
    val isWaitingForCapture: LiveData<Boolean> = _isWaitingForCapture

    private val _captureTrigger = MutableLiveData<Unit>()
    val captureTrigger: LiveData<Unit> = _captureTrigger

    private val sessionImagePaths = mutableListOf<String>()

    private val workoutSequenceMap = mutableMapOf<Int, SequenceModel>()

    private val _yogaPoseDataList = MutableLiveData<List<YogaPoseModel>>()
    val yogaPoseDataList: LiveData<List<YogaPoseModel>> = _yogaPoseDataList

    private val _currentPoseName = MutableLiveData<String>("Đang chờ...")
    val currentPoseName: LiveData<String> = _currentPoseName

    private val _currentGuideText = MutableLiveData<String>()
    val currentGuideText: LiveData<String> = _currentGuideText

    private val _timerText = MutableLiveData("00:00")
    val timerText: LiveData<String> = _timerText

    private val _detectedPoseId = MutableLiveData(-1)
    val detectedPoseId: LiveData<Int> = _detectedPoseId

    private val _previewPoseId = MutableLiveData(-1)
    val previewPoseId: LiveData<Int> = _previewPoseId

    private val _speakCommand = MutableLiveData<String>()
    val speakCommand: LiveData<String> = _speakCommand

    private var lastPoseName: String? = null
    private var poseStartTime: Long = 0
    private var isTrackingStarted = false
    private var exerciseTimer: Timer? = null

    // Biến quan trọng giống SingleYoga
    private var isCurrentlyCorrect = false
    private var currentPoseTotalSeconds = 0
    private val PREPARATION_TIME_MS = 3000L


fun toggleCaptureWait() {
    _isWaitingForCapture.value = !(_isWaitingForCapture.value ?: false)
}

    fun addCapturedImage(path: String) {
        sessionImagePaths.add(path)
    }

    fun getCapturedImages(): List<String> = sessionImagePaths

    fun processCoachLogic(result: PoseLandmarkerResult) {
        val poseId = _detectedPoseId.value ?: -1
        if (poseId != -1 && isTrackingStarted) {
            val (isCorrect, feedback) = YogaCoachUtils.getCoachFeedback(poseId, result)
            isCurrentlyCorrect = isCorrect

            // LOGIC CHỤP ẢNH: Nếu đang đợi chụp + đứng đúng tư thế
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

    // Cập nhật hàm clearData
    fun clearData() {
        workoutSequenceMap.clear()
        sessionImagePaths.clear() // Xóa ảnh khi kết thúc
        currentPoseTotalSeconds = 0
        _isWaitingForCapture.value = false
        lastPoseName = null
        stopExerciseTimer()
    }
    fun fetchYogaPoses() {
        YogaDataUtils.getRemoteYogaPoses { poses ->
            poses?.let { _yogaPoseDataList.postValue(it) }
        }
    }

    fun handlePoseInference(poseId: Int) {
        val allPoses = _yogaPoseDataList.value ?: return
        val poseData = allPoses.find { it.id == poseId } ?: return
        val currentPose = poseData.name

        if (currentPose == lastPoseName) {
            val elapsedTime = System.currentTimeMillis() - poseStartTime

            if (elapsedTime >= PREPARATION_TIME_MS) {
                if (!isTrackingStarted) {
                    isTrackingStarted = true

                    // Lấy thời gian cũ nếu tập lại bài này
                    currentPoseTotalSeconds = workoutSequenceMap[poseId]?.let {
                        timeStringToSeconds(it.duration)
                    } ?: 0

                    // Bắt đầu Timer logic (Timer luôn chạy nhưng chỉ cộng giây khi isCurrentlyCorrect = true)
                    startLogicalTimer()

                    if (!workoutSequenceMap.containsKey(poseId)) {
                        workoutSequenceMap[poseId] = SequenceModel(
                            id = poseData.id.toString(),
                            name = poseData.name,
                            photoUrl = poseData.photo_url,
                            duration = "00:00"
                        )
                    }

                    _previewPoseId.postValue(poseId)
                    _currentPoseName.postValue(currentPose)
                    _speakCommand.postValue("Bắt đầu tập $currentPose")
                }
                _detectedPoseId.postValue(poseId)
            } else {
                val countdown = 3 - (elapsedTime / 1000)
                _currentGuideText.postValue("Giữ nguyên $currentPose ($countdown s)")
            }
        } else {
            lastPoseName = currentPose
            poseStartTime = System.currentTimeMillis()
            _detectedPoseId.postValue(-1)
            _previewPoseId.postValue(-1)
            isCurrentlyCorrect = false // Reset trạng thái đúng/sai
            if (isTrackingStarted) stopExerciseTimer()
            _currentGuideText.postValue("Chuẩn bị: $currentPose")
        }
    }


    private fun startLogicalTimer() {
        exerciseTimer?.cancel()
        exerciseTimer = Timer()
        exerciseTimer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                // CHỈ CỘNG GIÂY KHI TƯ THẾ ĐANG ĐÚNG
                if (isCurrentlyCorrect) {
                    val currentId = _detectedPoseId.value ?: return
                    if (currentId == -1) return

                    currentPoseTotalSeconds++
                    val min = currentPoseTotalSeconds / 60
                    val sec = currentPoseTotalSeconds % 60
                    val timeStr = String.format("%02d:%02d", min, sec)

                    _timerText.postValue(timeStr)
                    workoutSequenceMap[currentId]?.duration = timeStr
                }
            }
        }, 1000, 1000)
    }

    fun stopExerciseTimer() {
        isTrackingStarted = false
        isCurrentlyCorrect = false
        exerciseTimer?.cancel()
        exerciseTimer = null
        _timerText.postValue("00:00")
        _detectedPoseId.postValue(-1)
    }

    fun getFinalSequenceList(): ArrayList<SequenceModel> {
        return ArrayList(workoutSequenceMap.values.toList())
    }


    private fun timeStringToSeconds(time: String): Int {
        val parts = time.split(":")
        return if (parts.size == 2) parts[0].toInt() * 60 + parts[1].toInt() else 0
    }

    override fun onCleared() {
        clearData()
        super.onCleared()
    }
}