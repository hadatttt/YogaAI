package com.hadat.aiyoga.community_mysequence

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.hadat.aiyoga.community.CommunityCategory
import com.hadat.aiyoga.data.firestore.repository.LikeRepository
import com.hadat.aiyoga.data.firestore.repository.SequenceRepository
import com.hadat.aiyoga.sequence.WorkoutSequenceModel
import hoang.dqm.codebase.base.viewmodel.BaseViewModel
import kotlinx.coroutines.launch

class CommunityMySequenceViewModel : BaseViewModel() {
    private val sequenceRepository = SequenceRepository()
    private val likeRepository = LikeRepository()

    val selectedCategory = MutableLiveData(CommunityCategory.LATEST)
    val communitySequences = MutableLiveData<List<WorkoutSequenceModel>>(emptyList())
    val mySequences = MutableLiveData<List<WorkoutSequenceModel>>(emptyList())

    fun fetchAll(userId: String) {
        fetchMySequences(userId = userId)
        fetchCommunitySequences(userId = userId)
    }

    fun selectCategory(category: CommunityCategory, userId: String) {
        if (selectedCategory.value == category && communitySequences.value?.isNotEmpty() == true) return
        selectedCategory.value = category
        fetchCommunitySequences(category = category, userId = userId)
    }

    fun fetchCommunitySequences(
        category: CommunityCategory = selectedCategory.value ?: CommunityCategory.LATEST,
        userId: String
    ) {
        viewModelScope.launch {
            val list = when (category) {
                CommunityCategory.LATEST -> sequenceRepository.getCommunitySequences(excludeUserId = userId)
                CommunityCategory.TOP_LIKED -> sequenceRepository.getTopLikedCommunitySequences(
                    excludeUserId = userId,
                    limit = 50
                )
                CommunityCategory.TOP_VIEWED -> sequenceRepository.getTrendingCommunitySequences(
                    excludeUserId = userId,
                    limit = 50
                )
                CommunityCategory.LIKED_BY_ME -> {
                    if (userId.isBlank() || userId == "guest") emptyList()
                    else {
                        val likedIds = likeRepository.getLikedSequenceIds(userId).toSet()
                        if (likedIds.isEmpty()) emptyList()
                        else sequenceRepository.getCommunitySequences(excludeUserId = userId)
                            .filter { it.id in likedIds }
                    }
                }
            }
            communitySequences.postValue(list)
        }
    }

    fun fetchMySequences(userId: String) {
        if (userId.isBlank() || userId == "guest") {
            mySequences.postValue(emptyList())
            return
        }

        viewModelScope.launch {
            mySequences.postValue(sequenceRepository.getMySequences(userId))
        }
    }
}

