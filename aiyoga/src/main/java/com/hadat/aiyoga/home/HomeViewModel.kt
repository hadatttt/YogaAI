package com.hadat.aiyoga.home

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.hadat.aiyoga.yogamain.YogaPoseModel
import com.hadat.aiyoga.yogautils.YogaDataUtils
import com.hadat.firestore.model.User
import com.hadat.firestore.repository.UserRepository
import hoang.dqm.codebase.base.viewmodel.BaseViewModel
import kotlinx.coroutines.launch

class HomeViewModel : BaseViewModel() {
    private val userRepository = UserRepository()
    private val auth = FirebaseAuth.getInstance()
    val userData = MutableLiveData<User?>()
    val categoryList = MutableLiveData<List<CategoryModel>>()
    val yogaPoseList = MutableLiveData<List<YogaPoseModel>>()
    private var fullYogaList = listOf<YogaPoseModel>()
    fun fetchUserData() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            val user = userRepository.getUser(uid)
            userData.postValue(user)
        }
    }
    fun fetchData() {
        fetchUserData()
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