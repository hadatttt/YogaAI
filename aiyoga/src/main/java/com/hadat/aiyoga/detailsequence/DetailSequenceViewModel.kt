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

    fun refreshSequence() {
        val current = sequenceData.value ?: return
        if (current.id.isBlank()) return

        viewModelScope.launch {
            sequenceRepository.getSequenceById(current.id)?.let {
                sequenceData.postValue(it)
            }
        }
    }

    fun setIsPublic(isPublic: Boolean) {
        val current = sequenceData.value ?: return
        if (current.id.isBlank()) return
        if (current.isPublic == isPublic) return

        visibilityUpdating.value = true
        val updated = current.copy(isPublic = isPublic)

        viewModelScope.launch {
            val ok = sequenceRepository.updateSequence(updated)
            if (ok) {
                sequenceData.postValue(updated)
            }
            visibilityUpdating.postValue(false)
        }
    }
}