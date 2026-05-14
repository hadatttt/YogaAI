package com.hadat.aiyoga.single_normalyoga

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import hoang.dqm.codebase.base.viewmodel.BaseViewModel
import java.util.Locale

class SingleNormalYogaViewModel : BaseViewModel() {

    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private var totalSeconds = 0

    private val _timerText = MutableLiveData("00:00")
    val timerText: LiveData<String> = _timerText

    private val _isTrackingStarted = MutableLiveData(false)
    val isTrackingStarted: LiveData<Boolean> = _isTrackingStarted

    private val timerRunnable = object : Runnable {
        override fun run() {
            totalSeconds++
            _timerText.value = formatTime(totalSeconds)
            handler.postDelayed(this, 1000)
        }
    }

    fun startTracking() {
        if (_isTrackingStarted.value == true) return
        _isTrackingStarted.value = true
        handler.post(timerRunnable)
    }

    fun stopTracking() {
        handler.removeCallbacks(timerRunnable)
        _isTrackingStarted.value = false
    }

    fun resetTracking() {
        stopTracking()
        totalSeconds = 0
        _timerText.value = "00:00"
    }

    fun getTotalTimeStudied(): Int = totalSeconds

    private fun formatTime(seconds: Int): String {
        val min = seconds / 60
        val sec = seconds % 60
        return String.format(Locale.US, "%02d:%02d", min, sec)
    }

    override fun onCleared() {
        handler.removeCallbacks(timerRunnable)
        super.onCleared()
    }
}
