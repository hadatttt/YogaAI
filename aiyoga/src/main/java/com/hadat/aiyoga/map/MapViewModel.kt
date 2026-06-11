package com.hadat.aiyoga.map


import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.hadat.aiyoga.data.firestore.model.MapPostModel
import com.hadat.aiyoga.data.firestore.repository.MapRepository
import com.hadat.aiyoga.data.firestore.repository.UserRepository
import hoang.dqm.codebase.base.viewmodel.BaseViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MapViewModel : BaseViewModel() {

    private val mapRepository = MapRepository()
    private val userRepository = UserRepository()
    private var fetchPostsJob: Job? = null

    val posts = MutableLiveData<List<MapPostModel>>(emptyList())
    val myPosts = MutableLiveData<List<MapPostModel>>(emptyList())
    val deleteStatus = MutableLiveData<Boolean?>(null)

    fun fetchLatestPosts() {
        fetchPostsJob?.cancel()
        fetchPostsJob = viewModelScope.launch {
            val rawPosts = mapRepository.getLatestPosts()
            posts.postValue(enrichPosts(rawPosts))
        }
    }

    fun fetchPostsNear(lat: Double, lng: Double) {
        fetchPostsJob?.cancel()
        fetchPostsJob = viewModelScope.launch {
            val rawPosts = mapRepository.getPostsNear(lat, lng)
            posts.postValue(enrichPosts(rawPosts))
        }
    }

    fun fetchPostsInBounds(southLat: Double, northLat: Double, westLng: Double, eastLng: Double) {
        fetchPostsJob?.cancel()
        fetchPostsJob = viewModelScope.launch {
            val rawPosts = mapRepository.getPostsInBounds(southLat, northLat, westLng, eastLng)
            posts.postValue(enrichPosts(rawPosts))
        }
    }

    fun clearPosts() {
        fetchPostsJob?.cancel()
        posts.value = emptyList()
    }

    fun fetchMyPosts(userId: String) {
        viewModelScope.launch {
            myPosts.postValue(enrichPosts(mapRepository.getMyPosts(userId)))
        }
    }

    fun deleteMyPost(post: MapPostModel, userId: String) {
        viewModelScope.launch {
            val ok = mapRepository.deletePost(post.id)
            deleteStatus.postValue(ok)
            if (ok) {
                fetchMyPosts(userId)
                posts.value = posts.value.orEmpty().filterNot { it.id == post.id }
            }
        }
    }

    fun resetDeleteStatus() {
        deleteStatus.value = null
    }

    private suspend fun enrichPosts(input: List<MapPostModel>): List<MapPostModel> {
        if (input.isEmpty()) return emptyList()
        val userCache = mutableMapOf<String, Pair<String, String>>()

        return input.map { post ->
            if (post.userName.isNotBlank() && post.userAvatar.isNotBlank()) {
                return@map post
            }
            val cached = userCache[post.userId]
            if (cached != null) {
                return@map post.copy(
                    userName = cached.first,
                    userAvatar = cached.second
                )
            }
            val user = userRepository.getUser(post.userId)
            val name = user?.displayName.orEmpty()
            val avatar = user?.photoUrl.orEmpty()
            userCache[post.userId] = name to avatar
            post.copy(
                userName = name,
                userAvatar = avatar
            )
        }
    }
}
