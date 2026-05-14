package com.hadat.aiyoga.multi_normalyoga

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.hadat.aiyoga.R
import com.hadat.aiyoga.sequence.SequenceModel
import com.hadat.aiyoga.sequence.WorkoutSequenceModel
import com.hadat.aiyoga.utils.service.AppPreferences
import com.hadat.aiyoga.yoga_single.WorkoutResultModel
import hoang.dqm.codebase.base.viewmodel.BaseViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MultiNormalYogaViewModel : BaseViewModel() {

    private val handler = Handler(Looper.getMainLooper())
    private var sequencePoses: List<SequenceModel> = emptyList()
    private var currentPoseIndex = 0
    private var currentTargetSeconds = 0
    private var appContext: Context? = null
    private val workoutSessionTracker = mutableMapOf<Int, WorkoutResultModel>()

    private val _currentPose = MutableLiveData<SequenceModel?>()
    val currentPose: LiveData<SequenceModel?> = _currentPose

    private val _poseCountText = MutableLiveData("1/1")
    val poseCountText: LiveData<String> = _poseCountText

    private val _timerText = MutableLiveData("00:00")
    val timerText: LiveData<String> = _timerText

    private val _currentGuideText = MutableLiveData<String>()
    val currentGuideText: LiveData<String> = _currentGuideText

    private val _isTrackingStarted = MutableLiveData(false)
    val isTrackingStarted: LiveData<Boolean> = _isTrackingStarted

    private val _sessionCompleted = MutableLiveData<Unit?>()
    val sessionCompleted: LiveData<Unit?> = _sessionCompleted

    private val timerRunnable = object : Runnable {
        override fun run() {
            if (_isTrackingStarted.value == true) {
                val result = workoutSessionTracker[currentPoseIndex]
                if (result != null) {
                    result.durationInSeconds++
                    _timerText.value = formatTime(result.durationInSeconds)
                    if (currentTargetSeconds > 0 && result.durationInSeconds >= currentTargetSeconds) {
                        nextPose(appContext, autoStart = true)
                        return
                    }
                }
            }
            handler.postDelayed(this, 1000)
        }
    }

    fun startWorkout(context: Context, sequence: WorkoutSequenceModel) {
        resetData()
        appContext = context.applicationContext
        sequencePoses = sequence.poses
        if (sequencePoses.isNotEmpty()) {
            openPose(context, 0)
        }
    }

    fun startTracking(context: Context) {
        if (_isTrackingStarted.value == true) return
        ensureCurrentResult(context)
        _isTrackingStarted.value = true
        _currentGuideText.value = context.getString(R.string.hold_to_finish)
        handler.post(timerRunnable)
    }

    fun nextPose(context: Context? = null, autoStart: Boolean = false) {
        val nextIndex = currentPoseIndex + 1
        if (nextIndex < sequencePoses.size) {
            openPose(context, nextIndex)
            if (autoStart) {
                context?.let { ensureCurrentResult(it) }
                _isTrackingStarted.value = true
                _currentGuideText.value = context?.getString(R.string.hold_to_finish) ?: ""
                handler.post(timerRunnable)
            }
        } else {
            stopTracking()
            _sessionCompleted.postValue(Unit)
        }
    }

    fun clearSessionCompleted() {
        _sessionCompleted.value = null
    }

    fun stopTracking() {
        handler.removeCallbacks(timerRunnable)
        _isTrackingStarted.value = false
    }

    fun resetData() {
        stopTracking()
        workoutSessionTracker.clear()
        sequencePoses = emptyList()
        currentPoseIndex = 0
        currentTargetSeconds = 0
        appContext = null
        _currentPose.value = null
        _poseCountText.value = "1/1"
        _timerText.value = "00:00"
        _currentGuideText.value = ""
        _sessionCompleted.value = null
    }

    fun getFinalWorkoutResults(): Array<WorkoutResultModel> {
        return workoutSessionTracker.values
            .filter { it.durationInSeconds > 0 }
            .toTypedArray()
    }

    private fun openPose(context: Context?, index: Int) {
        handler.removeCallbacks(timerRunnable)
        _isTrackingStarted.value = false
        currentPoseIndex = index
        val pose = sequencePoses[index]
        currentTargetSeconds = durationToSeconds(pose.duration)
        _currentPose.value = pose
        _poseCountText.value = "${index + 1}/${sequencePoses.size}"
        _timerText.value = formatTime(workoutSessionTracker[index]?.durationInSeconds ?: 0)
        _currentGuideText.value = context?.getString(R.string.tap_to_start) ?: ""
    }

    private fun ensureCurrentResult(context: Context) {
        val pose = _currentPose.value ?: return
        workoutSessionTracker.getOrPut(currentPoseIndex) {
            WorkoutResultModel(
                userId = AppPreferences.getUserId(context) ?: "guest",
                poseId = pose.id,
                poseName = pose.name,
                poseUrl = pose.photoUrl ?: "",
                durationInSeconds = 0,
                date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()),
                errorCount = 0,
                workoutTimestamp = System.currentTimeMillis(),
                isAiMode = false
            )
        }
    }

    private fun durationToSeconds(duration: String): Int {
        val parts = duration.split(":")
        val min = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val sec = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return (min * 60) + sec
    }

    private fun formatTime(seconds: Int): String {
        val min = seconds / 60
        val sec = seconds % 60
        return String.format(Locale.US, "%02d:%02d", min, sec)
    }

    override fun onCleared() {
        resetData()
        super.onCleared()
    }
}
