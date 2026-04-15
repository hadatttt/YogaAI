package com.hadat.aiyoga.result

import android.content.Context
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.hadat.aiyoga.data.firestore.model.MapPostModel
import com.hadat.aiyoga.data.firestore.model.User
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

class ResultViewModel: BaseViewModel() {

    private val workoutRepository = WorkoutRepository()
    private val mapRepository = MapRepository()
    private val userRepository = UserRepository()

    val workoutHistory = MutableLiveData<List<WorkoutResultModel>>(emptyList())
    val currentUser = MutableLiveData<User?>()
    val shareStatus = MutableLiveData<Boolean?>(null)
    val shareLoading = MutableLiveData(false)

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
        imageUris: List<String>,
        lat: Double,
        lng: Double,
        workout: WorkoutResultModel?
    ) {
        if (userId.isBlank() || imageUris.isEmpty()) {
            shareStatus.value = false
            return
        }

        shareLoading.value = true
        viewModelScope.launch {
            val uploaded = uploadAll(context, imageUris.map { Uri.parse(it) })
            if (uploaded.isEmpty()) {
                shareLoading.postValue(false)
                shareStatus.postValue(false)
                return@launch
            }

            val post = MapPostModel(
                userId = userId,
                userName = userName,
                userAvatar = userAvatar,
                description = description,
                imageUrls = uploaded,
                lat = lat,
                lng = lng,
                placeName = "",
                workoutId = "${workout?.poseId ?: 0}_${workout?.date.orEmpty()}",
                workoutTitle = workout?.poseName.orEmpty(),
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

    private suspend fun uploadAll(context: Context, uris: List<Uri>): List<String> = withContext(Dispatchers.IO) {
        val result = mutableListOf<String>()
        for (uri in uris) {
            val url = uploadOne(context, uri) ?: continue
            result.add(url)
        }
        result
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