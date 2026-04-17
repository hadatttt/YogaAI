package com.hadat.aiyoga.workoutoverview

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.hadat.aiyoga.data.firestore.repository.SequenceRepository
import com.hadat.aiyoga.sequence.WorkoutSequenceModel
import hoang.dqm.codebase.base.viewmodel.BaseViewModel
import kotlinx.coroutines.launch

class WorkoutOverviewViewModel : BaseViewModel() {
    private val sequenceRepository = SequenceRepository()

    val sequences = MutableLiveData<List<WorkoutSequenceModel>>(emptyList())

    fun fetchRange(userId: String, fromMillis: Long, toMillis: Long) {
        viewModelScope.launch {
            isLoading.postValue(true)
            val list = sequenceRepository.getMySequencesInRange(userId, fromMillis, toMillis)
            sequences.postValue(list)
            isLoading.postValue(false)
        }
    }
}