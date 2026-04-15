package com.hadat.aiyoga.yogamain

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import com.hadat.aiyoga.utils.yogautils.YogaCoachUtils
import com.hadat.aiyoga.utils.yogautils.YogaDataUtils
import hoang.dqm.codebase.base.viewmodel.BaseViewModel
import java.util.*

class YogaViewModel : BaseViewModel() {

    private val TAG = "YogaAI_Debug"

    // DANH SÁCH 82 TƯ THẾ CỐ ĐỊNH THEO MODEL L3
    private val YOGA_LABELS_FIXED = arrayOf(
        "Akarna Dhanurasana", "Bharadvajasana I", "Boat Pose", "Bound Angle Pose",
        "Bow Pose", "Bridge Pose", "Camel Pose", "Cat Cow Pose", "Chair Pose",
        "Child Pose", "Cobra Pose", "Cockerel Pose", "Corpse Pose", "Cow Face Pose",
        "Crane (Crow) Pose", "Dolphin Plank", "Dolphin Pose", "Downward-Facing Dog",
        "Eagle Pose", "Eight-Angle Pose", "Extended Puppy Pose", "Extended Revolved Side Angle",
        "Extended Revolved Triangle", "Feathered Peacock Pose", "Firefly Pose", "Fish Pose",
        "Four-Limbed Staff Pose", "Frog Pose", "Garland Pose", "Gate Pose", "Half Lord of the Fishes",
        "Half Moon Pose", "Handstand Pose", "Happy Baby Pose", "Head-to-Knee Forward Bend",
        "Heron Pose", "Intense Side Stretch", "Legs-Up-the-Wall Pose", "Locust Pose",
        "Lord of the Dance Pose", "Low Lunge Pose", "Noose Pose", "Peacock Pose",
        "Pigeon Pose", "Plank Pose", "Plow Pose", "Sage Koundinya", "Rajakapotasana",
        "Reclining Hand-to-Big-Toe", "Revolved Head-to-Knee", "Scale Pose", "Scorpion Pose",
        "Seated Forward Bend", "Shoulder-Pressing Pose", "Side-Reclining Leg Lift",
        "Side Crane (Crow) Pose", "Side Plank Pose", "Sitting pose 1", "Split pose",
        "Staff Pose", "Standing Forward Bend", "Standing Split Pose", "Standing big toe hold",
        "Supported Headstand", "Supported Shoulderstand", "Supta Baddha Konasana",
        "Supta Virasana Vajrasana", "Tortoise Pose", "Tree Pose", "Upward Bow (Wheel)",
        "Upward Facing Two-Foot Staff", "Upward Plank Pose", "Virasana", "Warrior III Pose",
        "Warrior II Pose", "Warrior I Pose", "Wide-Angle Seated Forward Bend",
        "Wide-Legged Forward Bend", "Wild Thing Pose", "Wind Relieving Pose", "Yogic sleep pose",
        "Reverse Warrior Pose"
    )

    private val _yogaPoseDataList = MutableLiveData<List<YogaPoseModel>>()
    val yogaPoseDataList: LiveData<List<YogaPoseModel>> = _yogaPoseDataList

    private val _currentPoseName = MutableLiveData<String>("Nhận diện...")
    val currentPoseName: LiveData<String> = _currentPoseName

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
    private val PREPARATION_TIME_MS = 2500L

    fun fetchYogaPoses() {
        YogaDataUtils.getRemoteYogaPoses { poses -> poses?.let { _yogaPoseDataList.postValue(it) } }
    }

    fun handlePoseInference(poseId: Int) {
        if (poseId < 0 || poseId >= YOGA_LABELS_FIXED.size) return

        val poseName = YOGA_LABELS_FIXED[poseId]

        if (poseName == lastPoseName) {
            val elapsedTime = System.currentTimeMillis() - poseStartTime
            if (elapsedTime >= PREPARATION_TIME_MS) {
                if (_isTrackingStarted.value == false) {
                    _isTrackingStarted.postValue(true)
                    startExerciseTimer()
                    _speakCommand.postValue("Bắt đầu tập $poseName")
                }
                if (_detectedPoseId.value != poseId) _detectedPoseId.postValue(poseId)
            } else {
                val countdown = ((PREPARATION_TIME_MS - elapsedTime) / 1000) + 1
                _currentGuideText.postValue("Chuẩn bị: $poseName ($countdown)")
            }
        } else {
            resetTracking(poseName, poseId)
        }
    }

    fun processCoachLogic(result: PoseLandmarkerResult) {
        val poseId = _detectedPoseId.value ?: -1
        if (poseId != -1 && _isTrackingStarted.value == true) {
            val (isCorrect, feedback) = YogaCoachUtils.getCoachFeedback(poseId, result)
            _currentGuideText.postValue(if (isCorrect) "✅ Tư thế đúng!" else "⚠️ $feedback")
            if (!isCorrect) _speakCommand.postValue(feedback)
        }
    }

    private fun resetTracking(poseName: String?, poseId: Int) {
        lastPoseName = poseName
        _currentPoseName.postValue(poseName ?: "Nhận diện...")
        poseStartTime = System.currentTimeMillis()
        stopExerciseTimer()
        _detectedPoseId.postValue(-1)
        _previewPoseId.postValue(poseId)

        if (poseName != null) {
            _currentGuideText.postValue("Bắt đầu cho: $poseName")
            _speakCommand.postValue("Sẵn sàng $poseName")
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