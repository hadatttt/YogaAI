package com.hadat.aiyoga.singlemode

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.hadat.aiyoga.firestore.repository.YogaRepository
import com.hadat.aiyoga.home.CategoryModel
import com.hadat.aiyoga.yogamain.YogaPoseModel
import com.hadat.aiyoga.yogautils.YogaDataUtils
import hoang.dqm.codebase.base.viewmodel.BaseViewModel
import kotlinx.coroutines.launch

class SingleModeViewModel : BaseViewModel() {

    private val yogaRepository = YogaRepository()

    val todayPickPose = MutableLiveData<YogaPoseModel?>()
    val categoryList = MutableLiveData<List<CategoryModel>>()
    val yogaPoseList = MutableLiveData<List<YogaPoseModel>>()

    private var fullYogaList = listOf<YogaPoseModel>()
    private var currentCategory = "All"
    private var currentSearchQuery = ""

    fun fetchData() {
        if (fullYogaList.isNotEmpty()) return

        YogaDataUtils.getRemoteYogaCategories { categories ->
            categories?.let { categoryList.postValue(it) }
        }

        YogaDataUtils.getRemoteYogaPoses { poses ->
            poses?.let {
                fullYogaList = it
                applyFilterAndSearch()
                fetchTodayPick()
            }
        }
    }

    fun trackPoseInteraction(pose: YogaPoseModel) {
        viewModelScope.launch {
            yogaRepository.trackInteraction(pose.id.toString(), pose.name)
        }
    }

    fun setCategory(categoryName: String) {
        currentCategory = categoryName
        applyFilterAndSearch()
    }

    fun setSearchQuery(query: String) {
        currentSearchQuery = query
        applyFilterAndSearch()
    }

    private fun applyFilterAndSearch() {
        val filteredList = fullYogaList.filter { pose ->
            val isMatchCategory = currentCategory.equals("All", ignoreCase = true) ||
                    pose.category.equals(currentCategory, ignoreCase = true)

            val isMatchSearch = currentSearchQuery.isEmpty() ||
                    pose.name.contains(currentSearchQuery, ignoreCase = true)

            isMatchCategory && isMatchSearch
        }
        yogaPoseList.postValue(filteredList)
    }

    private fun fetchTodayPick() {
        viewModelScope.launch {
            val trendingId = yogaRepository.getTodayTrendingPoseId()?.toString()

            val pose = if (trendingId != null && trendingId != "0") {
                fullYogaList.find { it.id.toString() == trendingId } ?: fullYogaList.firstOrNull()
            } else {
                fullYogaList.firstOrNull()
            }

            todayPickPose.postValue(pose)
        }
    }
}