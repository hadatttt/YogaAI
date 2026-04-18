package com.hadat.aiyoga.result

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.hadat.aiyoga.data.firestore.model.HealthProfileModel
import com.hadat.aiyoga.data.firestore.model.MapPostModel
import com.hadat.aiyoga.data.firestore.model.User
import com.hadat.aiyoga.data.firestore.repository.HealthProfileRepository
import com.hadat.aiyoga.data.firestore.repository.MapRepository
import com.hadat.aiyoga.data.firestore.repository.UserRepository
import com.hadat.aiyoga.data.firestore.repository.WorkoutRepository
import com.hadat.aiyoga.singleyoga.WorkoutResultModel
import com.hadat.aiyoga.utils.CloudinaryUtils
import hoang.dqm.codebase.base.viewmodel.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

class ResultViewModel : BaseViewModel() {

    private val workoutRepository = WorkoutRepository()
    private val mapRepository = MapRepository()
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

    fun sharePlace(
        context: Context,
        userId: String,
        userName: String,
        userAvatar: String,
        description: String,
        imageUri: String,
        lat: Double,
        lng: Double
    ) {
        if (userId.isBlank() || imageUri.isBlank()) {
            shareStatus.value = false
            return
        }

        shareLoading.value = true
        viewModelScope.launch {
            val uploadedUrl = uploadOne(context, Uri.parse(imageUri))

            if (uploadedUrl == null) {
                shareLoading.postValue(false)
                shareStatus.postValue(false)
                return@launch
            }

            val post = MapPostModel(
                userId = userId,
                userName = userName,
                userAvatar = userAvatar,
                description = description,
                imageUrls = uploadedUrl,
                lat = lat,
                lng = lng,
                createdAt = null
            )

            val ok = mapRepository.createPost(post)
            shareLoading.postValue(false)
            shareStatus.postValue(ok)
        }
    }

    fun resetShareStatus() {
        shareStatus.value = null
    }

    private suspend fun uploadOne(context: Context, uri: Uri): String? =
        suspendCancellableCoroutine { cont ->
            CloudinaryUtils.uploadImage(
                context = context,
                imageUri = uri,
                onSuccess = { if (cont.isActive) cont.resume(it) },
                onError = { if (cont.isActive) cont.resume(null) }
            )
        }
}