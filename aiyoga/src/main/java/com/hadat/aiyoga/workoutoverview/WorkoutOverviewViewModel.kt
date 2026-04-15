package com.hadat.aiyoga.workoutoverview

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.hadat.aiyoga.data.firestore.repository.WorkoutRepository
import com.hadat.aiyoga.singleyoga.WorkoutResultModel
import hoang.dqm.codebase.base.viewmodel.BaseViewModel
import kotlinx.coroutines.launch

class WorkoutOverviewViewModel : BaseViewModel() {
    private val workoutRepository = WorkoutRepository()

    val results = MutableLiveData<List<WorkoutResultModel>>(emptyList())
    val summaryText = MutableLiveData("")

    fun fetchRange(userId: String, fromMillis: Long, toMillis: Long) {
        viewModelScope.launch {
            val list = workoutRepository.getHistoryInRange(userId, fromMillis, toMillis)
            results.postValue(list)
            val totalSeconds = list.sumOf { it.durationInSeconds }
            val totalError = list.sumOf { it.errorCount }
            val calories = totalSeconds * 0.15f
            val accuracy = (100f - (totalError * 5f)).coerceIn(10f, 100f)
            summaryText.postValue(
                "Calories: %.1f kcal | Time: %ds | Accuracy: %d%%".format(calories, totalSeconds, accuracy.toInt())
            )
        }
    }
}

