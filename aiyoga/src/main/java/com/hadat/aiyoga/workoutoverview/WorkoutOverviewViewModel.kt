package com.hadat.aiyoga.workoutoverview

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.hadat.aiyoga.data.firestore.repository.HealthProfileRepository
import com.hadat.aiyoga.data.firestore.repository.WorkoutRepository
import com.hadat.aiyoga.singleyoga.WorkoutResultModel
import com.hadat.aiyoga.utils.yogautils.HealthCalculatorUtils
import com.hadat.aiyoga.utils.yogautils.YogaDataUtils
import hoang.dqm.codebase.base.viewmodel.BaseViewModel
import kotlinx.coroutines.launch
class WorkoutOverviewViewModel : BaseViewModel() {

    private val workoutRepository = WorkoutRepository()
    private val healthRepository = HealthProfileRepository()

    val userWeight = MutableLiveData<Float>(60f)
    val dailyGoalCalories = MutableLiveData<Int>(300)
    val sequences = MutableLiveData<List<WorkoutResultModel>>(emptyList())
    val metDataMap = MutableLiveData<Map<Int, Double>>()

    fun fetchRange(userId: String, fromMillis: Long, toMillis: Long) {
        viewModelScope.launch {
            isLoading.postValue(true)

            val profile = healthRepository.getProfile(userId)
            profile?.let {
                userWeight.postValue(it.weight)
                dailyGoalCalories.postValue(
                    HealthCalculatorUtils.calculateDailyGoalCalories(it.tdee)
                )
            }

            YogaDataUtils.getAllRemoteMet { map ->
                metDataMap.postValue(map)

                viewModelScope.launch {
                    val list = workoutRepository.getHistoryInRange(
                        userId,
                        fromMillis,
                        toMillis
                    )
                    sequences.postValue(list)
                    isLoading.postValue(false)
                }
            }
        }
    }
}