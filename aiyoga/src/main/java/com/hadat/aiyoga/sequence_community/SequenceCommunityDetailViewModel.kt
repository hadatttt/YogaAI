package com.hadat.aiyoga.sequence_community

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.hadat.aiyoga.data.firestore.repository.LikeRepository
import com.hadat.aiyoga.data.firestore.repository.SequenceRepository
import com.hadat.aiyoga.sequence.WorkoutSequenceModel
import hoang.dqm.codebase.base.viewmodel.BaseViewModel
import kotlinx.coroutines.launch

class SequenceCommunityDetailViewModel : BaseViewModel() {

    private val likeRepository = LikeRepository()
    private val sequenceRepository = SequenceRepository()

    val sequenceData = MutableLiveData<WorkoutSequenceModel>()
    val isLiked = MutableLiveData(false)
    val likeCount = MutableLiveData(0)
    val viewCount = MutableLiveData(0)

    val copyStatus = MutableLiveData<Boolean?>(null)

    fun setDetailData(data: WorkoutSequenceModel, userId: String) {
        sequenceData.value = data
        likeCount.value = data.likeCount
        viewCount.value = data.viewCount

        if (data.id.isNotBlank()) {
            viewModelScope.launch {
                likeRepository.increaseViewCount(data.id)
                viewCount.postValue((viewCount.value ?: data.viewCount) + 1)
            }
        }

        if (userId.isNotBlank() && data.id.isNotBlank()) {
            viewModelScope.launch {
                val liked = likeRepository.checkIsLiked(data.id, userId)
                isLiked.postValue(liked)
            }
        }
    }

    fun toggleLike(userId: String) {
        val current = sequenceData.value ?: return
        if (userId.isBlank() || current.id.isBlank()) return

        viewModelScope.launch {
            val liked = likeRepository.toggleLike(current.id, userId) ?: return@launch
            isLiked.postValue(liked)

            val currentCount = likeCount.value ?: current.likeCount
            val nextCount = if (liked) currentCount + 1 else (currentCount - 1).coerceAtLeast(0)
            likeCount.postValue(nextCount)
        }
    }

    fun copyToMySequences(userId: String) {
        val current = sequenceData.value ?: return
        if (userId.isBlank()) {
            copyStatus.value = false
            return
        }

        val copy = WorkoutSequenceModel(
            userId = userId,
            title = current.title,
            coverImageUrl = current.coverImageUrl,
            totalDuration = current.totalDuration,
            level = current.level,
            isPublic = false,
            poses = current.poses,
            createdAt = null
        )

        viewModelScope.launch {
            val ok = sequenceRepository.saveSequence(copy)
            copyStatus.postValue(ok)
        }
    }

    fun resetCopyStatus() {
        copyStatus.value = null
    }
}

