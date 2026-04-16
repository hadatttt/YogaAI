package com.hadat.aiyoga.multimodeyoga

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import com.hadat.aiyoga.sequence.SequenceModel
import com.hadat.aiyoga.sequence.WorkoutSequenceModel
import com.hadat.aiyoga.singleyoga.WorkoutResultModel
import com.hadat.aiyoga.utils.yogautils.YogaCoachUtils
import hoang.dqm.codebase.base.viewmodel.BaseViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Timer
import java.util.TimerTask

class MultiModeYogaViewModel : BaseViewModel() {
    private data class PoseWorkoutSummary(
        val poseId: Int,
        val poseUrl: String,
        val poseName: String,
        val durationInSeconds: Int,
        val errorCount: Int
    )

    private val _currentPose = MutableLiveData<SequenceModel?>()
    val currentPose: LiveData<SequenceModel?> = _currentPose

    private val _currentPoseIndex = MutableLiveData(0)
    val currentPoseIndex: LiveData<Int> = _currentPoseIndex

    private val _poseCountText = MutableLiveData("1/1")
    val poseCountText: LiveData<String> = _poseCountText

    private val _currentGuideText = MutableLiveData<String>()
    val currentGuideText: LiveData<String> = _currentGuideText

    private val _timerText = MutableLiveData("00:00")
    val timerText: LiveData<String> = _timerText

    private val _isTrackingStarted = MutableLiveData(false)
    val isTrackingStarted: LiveData<Boolean> = _isTrackingStarted

    private val _speakCommand = MutableLiveData<String>()
    val speakCommand: LiveData<String> = _speakCommand

    private val _isWaitingForCapture = MutableLiveData(false)
    val isWaitingForCapture: LiveData<Boolean> = _isWaitingForCapture

    private val _captureTrigger = MutableLiveData<Unit>()
    val captureTrigger: LiveData<Unit> = _captureTrigger

    private val _sessionCompleted = MutableLiveData<Unit>()
    val sessionCompleted: LiveData<Unit> = _sessionCompleted

    private val sessionImagePaths = mutableListOf<String>()
    private val completedResults = mutableListOf<PoseWorkoutSummary>()

    private var sequencePoses: List<SequenceModel> = emptyList()
    private var exerciseTimer: Timer? = null
    private var currentPoseSeconds = 0
    private var currentTargetSeconds = 0
    private var currentErrorCount = 0
    private var isCurrentlyCorrect = false
    private var hasStartedCorrectPose = false
    private var isPreviousFrameCorrect = true
    private var isAdvancingPose = false

    fun startWorkout(sequence: WorkoutSequenceModel) {
        sequencePoses = sequence.poses
        completedResults.clear()
        sessionImagePaths.clear()

        if (sequencePoses.isEmpty()) {
            _sessionCompleted.postValue(Unit)
            return
        }

        openPose(0)
    }

    fun toggleCaptureWait() {
        _isWaitingForCapture.value = !(_isWaitingForCapture.value ?: false)
    }

    fun addCapturedImage(path: String) {
        sessionImagePaths.add(path)
    }

    fun getCapturedImages(): List<String> = sessionImagePaths.toList()

    fun processCoachLogic(result: PoseLandmarkerResult) {
        val current = _currentPose.value ?: return
        val poseId = current.id.toIntOrNull() ?: -1
        if (poseId == -1 || _isTrackingStarted.value != true || isAdvancingPose) return

        val (isCorrect, feedback) = YogaCoachUtils.getCoachFeedback(poseId, result)

        if (isCorrect) {
            hasStartedCorrectPose = true
        } else if (hasStartedCorrectPose && isPreviousFrameCorrect) {
            currentErrorCount++
        }

        isPreviousFrameCorrect = isCorrect
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
    }

    fun moveToNextPose() {
        if (isAdvancingPose) return
        completeCurrentPoseAndAdvance()
    }

    fun stopTracking() {
        exerciseTimer?.cancel()
        exerciseTimer = null
        _isTrackingStarted.postValue(false)
    }

    fun buildResultList(userId: String): Array<WorkoutResultModel> {
        val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        val now = System.currentTimeMillis()
        return completedResults.mapIndexed { index, item ->
            WorkoutResultModel(
                userId = userId,
                poseId = item.poseId,
                poseUrl = item.poseUrl,
                poseName = item.poseName,
                durationInSeconds = item.durationInSeconds,
                date = date,
                capturedImages = sessionImagePaths.toList(),
                errorCount = item.errorCount,
                workoutTimestamp = now + index
            )
        }.toTypedArray()
    }

    private fun openPose(index: Int) {
        val pose = sequencePoses.getOrNull(index)
        if (pose == null) {
            _sessionCompleted.postValue(Unit)
            return
        }

        currentPoseSeconds = 0
        currentErrorCount = 0
        currentTargetSeconds = durationToSeconds(pose.duration)
        isCurrentlyCorrect = false
        hasStartedCorrectPose = false
        isPreviousFrameCorrect = true
        isAdvancingPose = false

        _currentPoseIndex.postValue(index)
        _currentPose.postValue(pose)
        _poseCountText.postValue("${index + 1}/${sequencePoses.size}")
        _timerText.postValue("00:00")
        _isTrackingStarted.postValue(true)
        _currentGuideText.postValue("Vào tư thế để bắt đầu tính giờ!")
        _speakCommand.postValue("Bắt đầu ${pose.name}")

        startLogicalTimer()
    }

    private fun completeCurrentPoseAndAdvance() {
        val current = _currentPose.value ?: return
        isAdvancingPose = true

        completedResults.add(
            PoseWorkoutSummary(
                poseId = current.id.toIntOrNull() ?: -1,
                poseUrl = current.photoUrl,
                poseName = current.name,
                durationInSeconds = currentPoseSeconds,
                errorCount = currentErrorCount
            )
        )

        val nextIndex = (_currentPoseIndex.value ?: 0) + 1
        if (nextIndex >= sequencePoses.size) {
            stopTracking()
            _sessionCompleted.postValue(Unit)
        } else {
            openPose(nextIndex)
        }
    }

    private fun startLogicalTimer() {
        exerciseTimer?.cancel()
        exerciseTimer = Timer()
        exerciseTimer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                if (isCurrentlyCorrect && !isAdvancingPose) {
                    currentPoseSeconds++
                    updateTimerUI()
                    if (currentTargetSeconds > 0 && currentPoseSeconds >= currentTargetSeconds) {
                        completeCurrentPoseAndAdvance()
                    }
                }
            }
        }, 1000, 1000)
    }

    private fun updateTimerUI() {
        val min = currentPoseSeconds / 60
        val sec = currentPoseSeconds % 60
        _timerText.postValue(String.format("%02d:%02d", min, sec))
    }

    private fun durationToSeconds(duration: String): Int {
        val parts = duration.split(":")
        val min = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val sec = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return (min * 60) + sec
    }

    override fun onCleared() {
        exerciseTimer?.cancel()
        super.onCleared()
    }
}

