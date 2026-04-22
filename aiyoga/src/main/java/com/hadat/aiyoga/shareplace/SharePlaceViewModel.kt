package com.hadat.aiyoga.shareplace

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.hadat.aiyoga.data.firestore.model.MapPostModel
import com.hadat.aiyoga.data.firestore.model.User
import com.hadat.aiyoga.data.firestore.repository.MapRepository
import com.hadat.aiyoga.data.firestore.repository.UserRepository
import com.hadat.aiyoga.utils.CloudinaryUtils
import hoang.dqm.codebase.base.viewmodel.BaseViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class SharePlaceViewModel : BaseViewModel() {

    private val mapRepository = MapRepository()
    private val userRepository = UserRepository()

    private val _capturedImages = MutableLiveData<List<String>>()
    val capturedImages: LiveData<List<String>> = _capturedImages

    private val _selectedImage = MutableLiveData<String>()
    val selectedImage: LiveData<String> = _selectedImage

    private val _currentUser = MutableLiveData<User?>()
    val currentUser: LiveData<User?> = _currentUser

    private val _shareStatus = MutableLiveData<Boolean?>(null)
    val shareStatus: LiveData<Boolean?> = _shareStatus

    private val _shareLoading = MutableLiveData(false)
    val shareLoading: LiveData<Boolean> = _shareLoading

    fun initData(images: List<String>, userId: String) {
        _capturedImages.value = images
        if (images.isNotEmpty() && _selectedImage.value == null) {
            _selectedImage.value = images.first()
        }
        loadCurrentUser(userId)
    }

    private fun loadCurrentUser(userId: String) {
        if (userId.isBlank()) return
        viewModelScope.launch {
            _currentUser.postValue(userRepository.getUser(userId))
        }
    }

    fun selectImage(uri: String) {
        _selectedImage.value = uri
    }

    fun updateCroppedImage(newUri: String) {
        _selectedImage.value = newUri
    }

    fun sharePlace(
        context: Context,
        userId: String,
        description: String,
        lat: Double,
        lng: Double
    ) {
        val imageUri = _selectedImage.value
        if (userId.isBlank() || imageUri.isNullOrBlank()) {
            _shareStatus.value = false
            return
        }

        _shareLoading.value = true
        viewModelScope.launch {
            val uploadedUrl = uploadOne(context, Uri.parse(imageUri))

            if (uploadedUrl == null) {
                _shareLoading.postValue(false)
                _shareStatus.postValue(false)
                return@launch
            }
            val user = _currentUser.value
            val post = MapPostModel(
                userId = userId,
                userName = user?.displayName ?: "Yoga User",
                userAvatar = user?.photoUrl.orEmpty(),
                description = description,
                imageUrls = uploadedUrl,
                lat = lat,
                lng = lng,
                createdAt = null
            )
            val ok = mapRepository.createPost(post)
            _shareLoading.postValue(false)
            _shareStatus.postValue(ok)
        }
    }

    fun resetShareStatus() {
        _shareStatus.value = null
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