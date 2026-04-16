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

    fun fetchRange(userId: String, fromMillis: Long, toMillis: Long) {
        viewModelScope.launch {
            val list = workoutRepository.getHistoryInRange(userId, fromMillis, toMillis)
            results.postValue(list)
        }
    }
}