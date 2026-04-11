package com.hadat.aiyoga.home

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.hadat.aiyoga.data.firestore.model.User
import com.hadat.aiyoga.data.firestore.repository.UserRepository
import com.hadat.aiyoga.data.firestore.repository.YogaRepository
import com.hadat.aiyoga.yogamain.YogaPoseModel
import com.hadat.aiyoga.utils.yogautils.YogaDataUtils
import hoang.dqm.codebase.base.viewmodel.BaseViewModel
import kotlinx.coroutines.launch

class HomeViewModel : BaseViewModel() {
    private val userRepository = UserRepository()
    private val yogaRepository = YogaRepository()
    private val auth = FirebaseAuth.getInstance()

    val userData = MutableLiveData<User?>()
    val todayPickPose = MutableLiveData<YogaPoseModel?>()
    private var fullYogaList = listOf<YogaPoseModel>()

    fun fetchData() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            val user = userRepository.getUser(uid)
            userData.postValue(user)
            YogaDataUtils.getRemoteYogaPoses { poses ->
                poses?.let {
                    fullYogaList = it
                    fetchTodayPick()
                }
            }
        }
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