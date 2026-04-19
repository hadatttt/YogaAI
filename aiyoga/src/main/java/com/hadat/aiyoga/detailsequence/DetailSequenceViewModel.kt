package com.hadat.aiyoga.detailsequence

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.hadat.aiyoga.data.firestore.repository.SequenceRepository
import com.hadat.aiyoga.sequence.WorkoutSequenceModel
import hoang.dqm.codebase.base.viewmodel.BaseViewModel
import kotlinx.coroutines.launch

class DetailSequenceViewModel : BaseViewModel() {
    private val sequenceRepository = SequenceRepository()

    val sequenceData = MutableLiveData<WorkoutSequenceModel>()
    val visibilityUpdating = MutableLiveData(false)

    fun setDetailData(data: WorkoutSequenceModel) {
        sequenceData.value = data
    }

    fun refreshSequence(id: String) {
        if (id.isBlank()) return

        viewModelScope.launch {
            sequenceRepository.getSequenceById(id)?.let { updatedSequence ->
                sequenceData.postValue(updatedSequence)
            }
        }
    }

    fun setIsPublic(newStatus: Boolean) {
        val current = sequenceData.value ?: return
        if (current.id.isBlank()) return
        visibilityUpdating.value = true
        viewModelScope.launch {
            val updated = current.copy(isPublic = newStatus)
            val isSuccess = sequenceRepository.updateSequence(updated)
            if (isSuccess) {
                sequenceData.postValue(updated)
            } else {
                sequenceData.postValue(current)
            }
            visibilityUpdating.postValue(false)
        }
    }
}