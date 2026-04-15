package com.hadat.aiyoga.detailsequence

import androidx.lifecycle.MutableLiveData
import com.hadat.aiyoga.sequence.WorkoutSequenceModel
import hoang.dqm.codebase.base.viewmodel.BaseViewModel

class DetailSequenceViewModel : BaseViewModel() {
    val sequenceData = MutableLiveData<WorkoutSequenceModel>()

    fun setDetailData(data: WorkoutSequenceModel) {
        sequenceData.value = data
    }
}