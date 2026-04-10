package com.hadat.aiyoga.createposes

import androidx.lifecycle.MutableLiveData
import com.hadat.aiyoga.home.CategoryModel
import com.hadat.aiyoga.yogamain.YogaPoseModel
import com.hadat.aiyoga.yogautils.YogaDataUtils
import hoang.dqm.codebase.base.viewmodel.BaseViewModel

class ChoosePoseViewModel : BaseViewModel() {

    val categoryList = MutableLiveData<List<CategoryModel>>()
    val yogaPoseList = MutableLiveData<List<YogaPoseModel>>()
    val selectedPoses = MutableLiveData<MutableList<YogaPoseModel>>(mutableListOf())

    val selectedIds = MutableLiveData<MutableList<Int>>(mutableListOf())

    private var fullYogaList = listOf<YogaPoseModel>()
    private var currentCategory = "All"
    private var currentSearchQuery = ""
    fun resetSelected() {
        selectedPoses.value = mutableListOf()
        selectedIds.value = mutableListOf()
    }
    fun fetchData() {
        if (fullYogaList.isNotEmpty()) return

        YogaDataUtils.getRemoteYogaCategories { categories ->
            categories?.let { categoryList.postValue(it) }
        }

        YogaDataUtils.getRemoteYogaPoses { poses ->
            poses?.let {
                fullYogaList = it
                applyFilterAndSearch()
            }
        }
    }

    fun addPose(pose: YogaPoseModel) {
        val currentList = selectedPoses.value ?: mutableListOf()
        val currentIds = selectedIds.value ?: mutableListOf()

        if (!currentIds.contains(pose.id)) {
            currentList.add(pose)
            currentIds.add(pose.id)

            selectedPoses.postValue(currentList)
            selectedIds.postValue(currentIds)
        }
    }

    fun removePose(pose: YogaPoseModel) {
        val currentList = selectedPoses.value ?: mutableListOf()
        val currentIds = selectedIds.value ?: mutableListOf()

        currentList.removeAll { it.id == pose.id }
        currentIds.remove(pose.id)

        selectedPoses.postValue(currentList)
        selectedIds.postValue(currentIds)
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
        val filtered = fullYogaList.filter { pose ->
            val matchCategory = currentCategory.equals("All", true) || pose.category.equals(currentCategory, true)
            val matchSearch = pose.name.contains(currentSearchQuery, true)
            matchCategory && matchSearch
        }
        yogaPoseList.postValue(filtered)
    }
}