package com.hadat.aiyoga.community

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.hadat.aiyoga.data.firestore.repository.LikeRepository
import com.hadat.aiyoga.data.firestore.repository.SequenceRepository
import com.hadat.aiyoga.sequence.WorkoutSequenceModel
import hoang.dqm.codebase.base.viewmodel.BaseViewModel
import kotlinx.coroutines.launch

class CommunityViewModel : BaseViewModel() {

    private val sequenceRepository = SequenceRepository()
    private val likeRepository = LikeRepository()

    val categories = MutableLiveData(
        listOf(
            CommunityCategory.LATEST,
            CommunityCategory.TOP_LIKED,
            CommunityCategory.TOP_VIEWED,
            CommunityCategory.LIKED_BY_ME
        )
    )

    val selectedCategory = MutableLiveData(CommunityCategory.LATEST)
    val sequences = MutableLiveData<List<WorkoutSequenceModel>>(emptyList())
    val loading = MutableLiveData(false)

    fun selectCategory(category: CommunityCategory, userId: String) {
        if (selectedCategory.value == category && sequences.value?.isNotEmpty() == true) return
        selectedCategory.value = category
        fetchSequences(category, userId)
    }

    fun fetchSequences(category: CommunityCategory = selectedCategory.value ?: CommunityCategory.LATEST, userId: String) {
        loading.value = true
        viewModelScope.launch {
            val list = when (category) {
                CommunityCategory.LATEST -> sequenceRepository.getAllSequences()
                CommunityCategory.TOP_LIKED -> sequenceRepository.getTopLikedSequences(limit = 50)
                CommunityCategory.TOP_VIEWED -> sequenceRepository.getTrendingSequences(limit = 50)
                CommunityCategory.LIKED_BY_ME -> {
                    if (userId.isBlank() || userId == "guest") {
                        emptyList()
                    } else {
                        val likedIds = likeRepository.getLikedSequenceIds(userId).toSet()
                        if (likedIds.isEmpty()) emptyList()
                        else sequenceRepository.getAllSequences().filter { it.id in likedIds }
                    }
                }
            }
            sequences.postValue(list)
            loading.postValue(false)
        }
    }
}

