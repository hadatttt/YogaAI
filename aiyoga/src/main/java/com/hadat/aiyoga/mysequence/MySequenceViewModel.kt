package com.hadat.aiyoga.mysequence

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.hadat.aiyoga.data.firestore.repository.SequenceRepository
import com.hadat.aiyoga.sequence.WorkoutSequenceModel
import hoang.dqm.codebase.base.viewmodel.BaseViewModel
import kotlinx.coroutines.launch

class MySequenceViewModel : BaseViewModel() {
    private val sequenceRepository = SequenceRepository()

    val mySequences = MutableLiveData<List<WorkoutSequenceModel>>(emptyList())

    fun fetchMySequences(userId: String) {
        if (userId.isBlank() || userId == "guest") {
            mySequences.postValue(emptyList())
            return
        }

        viewModelScope.launch {
            val list = sequenceRepository.getMySequences(userId)
            mySequences.postValue(list)
        }
    }
}
