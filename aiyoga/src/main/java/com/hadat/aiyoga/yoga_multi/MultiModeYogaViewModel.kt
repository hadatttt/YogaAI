package com.hadat.aiyoga.yoga_multi

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import com.hadat.aiyoga.R
import com.hadat.aiyoga.sequence.SequenceModel
import com.hadat.aiyoga.sequence.WorkoutSequenceModel
import com.hadat.aiyoga.yoga_single.WorkoutResultModel
import com.hadat.aiyoga.utils.service.AppPreferences
import com.hadat.aiyoga.utils.yogautils.YogaCoachUtils
import hoang.dqm.codebase.base.viewmodel.BaseViewModel
import java.text.SimpleDateFormat
import java.util.*

class MultiModeYogaViewModel : BaseViewModel() {

    private val _currentPose = MutableLiveData<SequenceModel?>()
    val currentPose: LiveData<SequenceModel?> = _currentPose

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

    private val _sessionCompleted = MutableLiveData<Unit?>()
    val sessionCompleted: LiveData<Unit?> = _sessionCompleted

    private val _nextPoseTrigger = MutableLiveData<Unit?>()
    val nextPoseTrigger: LiveData<Unit?> = _nextPoseTrigger

    private val sessionImagePaths = mutableListOf<String>()
    private val workoutSessionTracker = mutableMapOf<Int, WorkoutResultModel>()

    private var sequencePoses: List<SequenceModel> = emptyList()
    private var currentPoseIndex = 0
    private var currentTargetSeconds = 0

    @Volatile private var isCurrentlyCorrect = false
    private var hasStartedCorrectPose = false
    private var isPreviousFrameCorrect = true
    private var isAdvancingPose = false

    private val handler = Handler(Looper.getMainLooper())
    private var hasCapturedCurrentPose = false
    private var captureStartTime = 0L

    private var lastFeedback = ""
    private var lastFeedbackTime = 0L
    private val FEEDBACK_HOLD_TIME = 2000L

    private val timerRunnable = object : Runnable {
        override fun run() {
            if (isCurrentlyCorrect && !isAdvancingPose && _isTrackingStarted.value == true) {
                val currentPoseId = _currentPose.value?.id ?: -1
                if (currentPoseId != -1) {
                    workoutSessionTracker[currentPoseId]?.let { result ->
                        result.durationInSeconds++
                        _timerText.value = formatTime(result.durationInSeconds)

                        if (currentTargetSeconds > 0 && result.durationInSeconds >= currentTargetSeconds) {
                            handler.post { completeCurrentPoseAndAdvance() }
                            return
                        }
                    }
                }
            }
            handler.postDelayed(this, 1000)
        }
    }
    fun clearTriggers() {
        _sessionCompleted.value = null
        _nextPoseTrigger.value = null
    }
    fun processCoachLogic(context: Context, result: PoseLandmarkerResult) {
        val current = _currentPose.value ?: return
        if (_isTrackingStarted.value != true || isAdvancingPose) return

        val (isCorrect, feedback) = YogaCoachUtils.getCoachFeedback(context, current.id, result)
        isCurrentlyCorrect = isCorrect

        val currentResult = workoutSessionTracker.getOrPut(current.id) {
            WorkoutResultModel(
                userId = AppPreferences.getUserId(context) ?: "guest",
                poseId = current.id,
                poseName = current.name,
                poseUrl = current.photoUrl ?: "",
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

    fun startWorkout(context: Context, sequence: WorkoutSequenceModel) {
        resetData()
        clearTriggers()
        sequencePoses = sequence.poses
        if (sequencePoses.isEmpty()) {
            _sessionCompleted.postValue(Unit)
            return
        }
        openPose(context, 0)
    }

    private fun openPose(context: Context, index: Int) {
        val pose = sequencePoses.getOrNull(index) ?: return

        currentPoseIndex = index
        currentTargetSeconds = durationToSeconds(pose.duration)

        isCurrentlyCorrect = false
        hasStartedCorrectPose = false
        isPreviousFrameCorrect = true
        isAdvancingPose = false
        hasCapturedCurrentPose = false
        captureStartTime = 0L
        lastFeedback = ""

        _currentPose.postValue(pose)
        _poseCountText.postValue("${index + 1}/${sequencePoses.size}")
        _timerText.postValue("00:00")
        _currentGuideText.postValue(context.getString(R.string.guide_get_ready))
        _speakCommand.postValue("${context.getString(R.string.start_command)} ${pose.name}")
        _isTrackingStarted.postValue(true)

        startLogicalTimer()
    }

    private fun completeCurrentPoseAndAdvance() {
        if (isAdvancingPose) return
        isAdvancingPose = true
        handler.removeCallbacks(timerRunnable)

        val nextIndex = currentPoseIndex + 1
        if (nextIndex >= sequencePoses.size) {
            stopTracking()
            _sessionCompleted.postValue(Unit)
        } else {
            _nextPoseTrigger.postValue(Unit)
        }
    }

    fun nextPose(context: Context) {
        val nextIndex = currentPoseIndex + 1
        if (nextIndex < sequencePoses.size) {
            openPose(context, nextIndex)
        } else {
            stopTracking()
            _sessionCompleted.postValue(Unit)
        }
    }

    fun moveToNextPose(context: Context) {
        if (isAdvancingPose) return
        completeCurrentPoseAndAdvance()
        if (currentPoseIndex + 1 < sequencePoses.size) {
            nextPose(context)
        }
    }

    private fun startLogicalTimer() {
        handler.removeCallbacks(timerRunnable)
        handler.post(timerRunnable)
    }

    fun stopTracking() {
        handler.removeCallbacks(timerRunnable)
        isCurrentlyCorrect = false
        _isTrackingStarted.postValue(false)
    }

    fun showBodyNotReadyGuide(context: Context) {
        isCurrentlyCorrect = false
        captureStartTime = 0L
        _currentGuideText.postValue(context.getString(R.string.stand_back_full_body))
    }


    fun resetData() {
        stopTracking()
        handler.removeCallbacks(timerRunnable)
        sessionImagePaths.clear()
        workoutSessionTracker.clear()
        sequencePoses = emptyList()
        currentPoseIndex = 0
        currentTargetSeconds = 0
        isAdvancingPose = false
        isCurrentlyCorrect = false
        hasStartedCorrectPose = false
        isPreviousFrameCorrect = true
        hasCapturedCurrentPose = false
        captureStartTime = 0L
        lastFeedback = ""
        lastFeedbackTime = 0L

        _currentPose.value = null
        _currentGuideText.value = ""
        _speakCommand.value = ""
        _timerText.value = "00:00"
        _poseCountText.value = "1/1"
        _isWaitingForCapture.value = false
        _isTrackingStarted.value = false
        clearTriggers()
    }

    fun toggleCaptureWait() {
        _isWaitingForCapture.value = !(_isWaitingForCapture.value ?: false)
        if (_isWaitingForCapture.value == true) {
            captureStartTime = 0L
            hasCapturedCurrentPose = false
        }
    }

    private fun formatTime(seconds: Int): String {
        val min = seconds / 60
        val sec = seconds % 60
        return String.format(Locale.US, "%02d:%02d", min, sec)
    }

    private fun durationToSeconds(duration: String): Int {
        val parts = duration.split(":")
        val min = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val sec = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return (min * 60) + sec
    }

    fun addCapturedImage(path: String) = sessionImagePaths.add(path)
    fun getCapturedImages(): List<String> = sessionImagePaths.toList()

    fun getFinalWorkoutResults(): Array<WorkoutResultModel> {
        return workoutSessionTracker.values
            .filter { it.durationInSeconds > 0 }
            .toTypedArray()
    }

    override fun onCleared() {
        resetData()
        super.onCleared()
    }
}
