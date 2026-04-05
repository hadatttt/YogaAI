package com.hadat.aiyoga.home

import androidx.lifecycle.MutableLiveData
import com.hadat.aiyoga.yogamain.YogaPoseModel
import com.hadat.aiyoga.yogautils.YogaDataUtils
import hoang.dqm.codebase.base.viewmodel.BaseViewModel

class HomeViewModel : BaseViewModel() {

    val categoryList = MutableLiveData<List<CategoryModel>>()
    val yogaPoseList = MutableLiveData<List<YogaPoseModel>>()
    private var fullYogaList = listOf<YogaPoseModel>()

    fun fetchData() {
        if (fullYogaList.isNotEmpty()) return

        YogaDataUtils.getRemoteYogaCategories { categories ->
            categories?.let { categoryList.postValue(it) }
        }
        YogaDataUtils.getRemoteYogaPoses { poses ->
            poses?.let {
                fullYogaList = it
                yogaPoseList.postValue(it)
            }
        }
    }

    fun filterPoses(categoryName: String) {
        val filtered = if (categoryName.equals("All", ignoreCase = true)) {
            fullYogaList
        } else {
            fullYogaList.filter {
                it.category.equals(categoryName, ignoreCase = true)
            }
        }
        yogaPoseList.postValue(filtered)
    }
}