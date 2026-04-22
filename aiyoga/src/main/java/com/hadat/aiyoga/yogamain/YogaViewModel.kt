package com.hadat.aiyoga.yogamain

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import com.hadat.aiyoga.sequence.SequenceModel
import com.hadat.aiyoga.utils.yogautils.YogaCoachUtils
import com.hadat.aiyoga.data.remoteconfig.YogaDataUtils
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

    private val _currentPoseName = MutableLiveData<String>("...")
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

    private var isCurrentlyCorrect = false
    private var currentPoseTotalSeconds = 0
    private val PREPARATION_TIME_MS = 3000L


fun toggleCaptureWait() {
    _isWaitingForCapture.value = !(_isWaitingForCapture.value ?: false)
}

    fun addCapturedImage(path: String) {
        sessionImagePaths.add(path)
    }

    fun getCapturedImages(): List<String> = sessionImagePaths.toList()

    fun processCoachLogic(context: android.content.Context, result: PoseLandmarkerResult) {
        val poseId = _detectedPoseId.value ?: -1
        if (poseId != -1 && isTrackingStarted) {
            val (isCorrect, feedback) = YogaCoachUtils.getCoachFeedback(context, poseId, result)
            isCurrentlyCorrect = isCorrect

            if (isCorrect && _isWaitingForCapture.value == true) {
                _captureTrigger.postValue(Unit)
                _isWaitingForCapture.postValue(false)
            }
            if (isCorrect) {
                val msg = context.getString(com.hadat.aiyoga.R.string.guide_perfect_counting)
                _currentGuideText.postValue(msg)
            } else {
                _currentGuideText.postValue("⚠️ $feedback")
                _speakCommand.postValue(feedback)
            }
        } else {
            isCurrentlyCorrect = false
        }
    }

    fun clearData() {
        workoutSequenceMap.clear()
        sessionImagePaths.clear()
        currentPoseTotalSeconds = 0
        _isWaitingForCapture.value = false
        lastPoseName = null
        stopExerciseTimer()
    }
    fun fetchYogaPoses(context: Context) {
        YogaDataUtils.getRemoteYogaPoses(context.applicationContext) { poses ->
            poses?.let { _yogaPoseDataList.postValue(it) }
        }
    }

    fun handlePoseInference(context: android.content.Context, poseId: Int) {
        val allPoses = _yogaPoseDataList.value ?: return
        val poseData = allPoses.find { it.id == poseId } ?: return
        val currentPose = poseData.name

        if (currentPose == lastPoseName) {
            val elapsedTime = System.currentTimeMillis() - poseStartTime

            if (elapsedTime >= PREPARATION_TIME_MS) {
                if (!isTrackingStarted) {
                    isTrackingStarted = true

                    currentPoseTotalSeconds = workoutSequenceMap[poseId]?.let {
                        timeStringToSeconds(it.duration)
                    } ?: 0

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
                    val startMsg = "${context.getString(com.hadat.aiyoga.R.string.start_practicing)} $currentPose"
                    _speakCommand.postValue(startMsg)
                }
                _detectedPoseId.postValue(poseId)
            } else {
                val countdown = 3 - (elapsedTime / 1000)
                val holdMsg = "${context.getString(com.hadat.aiyoga.R.string.hold_pose)} $currentPose ($countdown s)"
                _currentGuideText.postValue(holdMsg)
            }
        } else {
            lastPoseName = currentPose
            poseStartTime = System.currentTimeMillis()
            _detectedPoseId.postValue(-1)
            _previewPoseId.postValue(-1)
            isCurrentlyCorrect = false
            if (isTrackingStarted) stopExerciseTimer()
            val prepareMsg = "${context.getString(com.hadat.aiyoga.R.string.prepare_pose)}: $currentPose"
            _currentGuideText.postValue(prepareMsg)
        }
    }


    private fun startLogicalTimer() {
        exerciseTimer?.cancel()
        exerciseTimer = Timer()
        exerciseTimer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
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
    fun resetData() {
        clearData()
        _currentPoseName.value = "..."
        _currentGuideText.value = ""
        _timerText.value = "00:00"
        _detectedPoseId.value = -1
        _previewPoseId.value = -1
        _isWaitingForCapture.value = false

        isTrackingStarted = false
        isCurrentlyCorrect = false
        poseStartTime = 0
        lastPoseName = null
    }
}