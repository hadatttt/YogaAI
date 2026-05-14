package com.hadat.aiyoga.workoutoverview

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.hadat.aiyoga.data.firestore.repository.HealthProfileRepository
import com.hadat.aiyoga.data.firestore.repository.WorkoutRepository
import com.hadat.aiyoga.yoga_single.WorkoutResultModel
import com.hadat.aiyoga.utils.yogautils.HealthCalculatorUtils
import com.hadat.aiyoga.data.remoteconfig.YogaDataUtils
import hoang.dqm.codebase.base.viewmodel.BaseViewModel
import kotlinx.coroutines.launch

class WorkoutOverviewViewModel : BaseViewModel() {

    private val workoutRepository = WorkoutRepository()
    private val healthRepository = HealthProfileRepository()

    val userWeight = MutableLiveData(60f)
    val dailyGoalCalories = MutableLiveData(300)
    val sequences = MutableLiveData<List<WorkoutResultModel>>(emptyList())
    val metDataMap = MutableLiveData<Map<Int, Double>>()

    fun fetchRange(userId: String, fromMillis: Long, toMillis: Long) {
        viewModelScope.launch {
            isLoading.value = true
            healthRepository.getProfile(userId)?.let {
                userWeight.value = it.weight
                dailyGoalCalories.value = HealthCalculatorUtils.calculateDailyGoalCalories(it.tdee)
            }
            metDataMap.value = YogaDataUtils.getAllMetData()
            sequences.value = workoutRepository.getHistoryInRange(userId, fromMillis, toMillis)
            isLoading.value = false
        }
    }
}