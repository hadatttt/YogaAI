package com.hadat.aiyoga.sequence

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.hadat.aiyoga.data.firestore.repository.SequenceRepository
import com.hadat.aiyoga.data.firestore.repository.UserRepository
import com.hadat.aiyoga.data.remoteconfig.YogaDataUtils
import com.hadat.aiyoga.utils.yogautils.YogaRecommender
import com.hadat.aiyoga.yoga_ai.YogaPoseModel
import hoang.dqm.codebase.base.viewmodel.BaseViewModel
import kotlinx.coroutines.launch

class SequencesViewModel : BaseViewModel() {
    private val repository = SequenceRepository()
    private val recommender = YogaRecommender()
    val lastSavedSequence = MutableLiveData<WorkoutSequenceModel>()
    private val userRepository = UserRepository()
    val sequenceList = MutableLiveData<MutableList<SequenceModel>>(mutableListOf())
    val recommendationList = MutableLiveData<List<YogaPoseModel>>()
    val saveStatus = MutableLiveData<Boolean?>()

    private var allPoses = listOf<YogaPoseModel>()
    fun resetSaveStatus() {
        saveStatus.value = null
    }
    fun fetchAllPoses(context: Context) {
        if (allPoses.isNotEmpty()) return
        YogaDataUtils.getRemoteYogaPoses(context.applicationContext) { poses ->
            poses?.let {
                allPoses = it
                getRecommendations()
            }
        }
    }

    private fun getRecommendations() {
        val currentList = sequenceList.value ?: return
        if (allPoses.isEmpty()) return
        val suggested = recommender.getRecommendations(currentList, allPoses)
        recommendationList.postValue(suggested)
    }

    fun updateList(newList: List<SequenceModel>) {
        sequenceList.value = newList.toMutableList()
        getRecommendations()
    }

    fun updateDuration(position: Int, newDuration: String) {
        sequenceList.value?.let {
            if (position in it.indices) {
                it[position].duration = newDuration
                sequenceList.value = it
            }
        }
    }

    fun saveSequence(title: String, level: Int, coverUrl: String, userId: String) {
        val currentPoses = sequenceList.value ?: emptyList()

        val totalSeconds = currentPoses.sumOf {
            val parts = it.duration.split(":")
            val mins = parts.getOrNull(0)?.toIntOrNull() ?: 0
            val secs = parts.getOrNull(1)?.toIntOrNull() ?: 0
            (mins * 60) + secs
        }

        viewModelScope.launch {
            val user = userRepository.getUser(userId)
            val authorName = user?.displayName ?: ""

            val finalSequence = WorkoutSequenceModel(
                userId = userId,
                authorName = authorName,
                title = title,
                coverImageUrl = coverUrl,
                totalDuration = String.format("%02d:%02d", totalSeconds / 60, totalSeconds % 60),
                level = level,
                isPublic = true,
                poses = currentPoses,
                createdAt = null
            )

            val result = repository.saveSequence(finalSequence)
            if (result) {
                lastSavedSequence.postValue(finalSequence)
                saveStatus.postValue(true)
            } else {
                saveStatus.postValue(false)
            }
        }
    }
    fun updateSequence(
        id: String,
        title: String,
        level: Int,
        coverUrl: String,
        userId: String,
        isPublic: Boolean
    ) {
        val currentPoses = sequenceList.value ?: emptyList()

        val totalSeconds = currentPoses.sumOf {
            val parts = it.duration.split(":")
            val mins = parts.getOrNull(0)?.toIntOrNull() ?: 0
            val secs = parts.getOrNull(1)?.toIntOrNull() ?: 0
            (mins * 60) + secs
        }

        val updatedSequence = WorkoutSequenceModel(
            id = id,
            userId = userId,
            title = title,
            coverImageUrl = coverUrl,
            totalDuration = String.format("%02d:%02d", totalSeconds / 60, totalSeconds % 60),
            level = level,
            isPublic = isPublic,
            poses = currentPoses,
            createdAt = null
        )

        viewModelScope.launch {
            val result = repository.updateSequence(updatedSequence)
            if (result) {
                lastSavedSequence.postValue(updatedSequence)
                saveStatus.postValue(true)
            } else {
                saveStatus.postValue(false)
            }
        }
    }
}
