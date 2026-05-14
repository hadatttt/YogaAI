package com.hadat.aiyoga.result

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.hadat.aiyoga.data.firestore.model.HealthProfileModel
import com.hadat.aiyoga.data.firestore.model.User
import com.hadat.aiyoga.data.firestore.repository.HealthProfileRepository
import com.hadat.aiyoga.data.firestore.repository.UserRepository
import com.hadat.aiyoga.data.firestore.repository.WorkoutRepository
import com.hadat.aiyoga.yoga_single.WorkoutResultModel
import hoang.dqm.codebase.base.viewmodel.BaseViewModel
import kotlinx.coroutines.launch

class ResultViewModel : BaseViewModel() {

    private val workoutRepository = WorkoutRepository()
    private val userRepository = UserRepository()
    private val healthRepository = HealthProfileRepository()
    private val _healthProfile = MutableLiveData<HealthProfileModel>()
    val healthProfile: LiveData<HealthProfileModel> = _healthProfile

    val workoutHistory = MutableLiveData<List<WorkoutResultModel>>(emptyList())
    val currentUser = MutableLiveData<User?>()
    val shareStatus = MutableLiveData<Boolean?>(null)
    val shareLoading = MutableLiveData(false)


    fun loadHealthProfile(userId: String) {
        viewModelScope.launch {
            val data = healthRepository.getProfile(userId)
            if (data != null) {
                _healthProfile.postValue(data)
            }
        }
    }
    fun saveWorkoutResults(list: List<WorkoutResultModel>, userId: String) {
        viewModelScope.launch {
            list.forEach { result ->
                workoutRepository.saveWorkoutResult(
                    result.copy(userId = userId)
                )
            }
        }
    }
    fun fetchWorkoutHistory(userId: String) {
        viewModelScope.launch {
            workoutHistory.postValue(workoutRepository.getWorkoutHistory(userId))
        }
    }

    fun loadCurrentUser(userId: String) {
        if (userId.isBlank()) return
        viewModelScope.launch {
            currentUser.postValue(userRepository.getUser(userId))
        }
    }
    fun resetShareStatus() {
        shareStatus.value = null
    }

}