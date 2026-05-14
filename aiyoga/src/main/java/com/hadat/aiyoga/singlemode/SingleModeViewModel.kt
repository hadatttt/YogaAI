package com.hadat.aiyoga.singlemode

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.hadat.aiyoga.data.firestore.repository.YogaRepository
import com.hadat.aiyoga.data.room.AppDatabase
import com.hadat.aiyoga.data.room.PoseMetadataEntity
import com.hadat.aiyoga.home.CategoryModel
import com.hadat.aiyoga.yoga_ai.YogaPoseModel
import com.hadat.aiyoga.data.remoteconfig.YogaDataUtils
import hoang.dqm.codebase.base.viewmodel.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SingleModeViewModel : BaseViewModel() {
    private val yogaRepository = YogaRepository()
    val todayPickPose = MutableLiveData<YogaPoseModel?>()
    val categoryList = MutableLiveData<List<CategoryModel>>()
    val yogaPoseList = MutableLiveData<List<YogaPoseModel>>()

    private var fullYogaList = listOf<YogaPoseModel>()
    private var currentCategory = "All"
    private var currentSearchQuery = ""
    private var loadedDataVersion = -1

    fun fetchData(context: Context, forceRefresh: Boolean = false) {
        if (!forceRefresh && fullYogaList.isNotEmpty() && loadedDataVersion == YogaDataUtils.dataVersion) return
        val localCategories = YogaDataUtils.getLocalYogaCategories()
        categoryList.value = localCategories.map { category ->
            category.copy(
                displayValue = YogaDataUtils.getLocalizedCategory(context, category.value)
            )
        }
        val poses = YogaDataUtils.getAllPoses()
        if (poses.isNotEmpty()) {
            fullYogaList = poses
            loadedDataVersion = YogaDataUtils.dataVersion
            applyFilterAndSearch(context)
            fetchTodayPick(context)
        }
    }

    fun trackPoseInteraction(pose: YogaPoseModel) {
        viewModelScope.launch {
            yogaRepository.trackInteraction(pose.id.toString(), pose.name)
        }
    }

    fun setCategory(context: Context, categoryName: String) {
        currentCategory = categoryName
        applyFilterAndSearch(context)
    }

    fun setSearchQuery(context: Context, query: String) {
        currentSearchQuery = query
        applyFilterAndSearch(context)
    }

    private fun fetchTodayPick(context: Context) {
        viewModelScope.launch {
            val trendingId = yogaRepository.getTodayTrendingPoseId()
            val pose = fullYogaList.find { it.id.toString() == trendingId } ?: fullYogaList.firstOrNull()

            pose?.let {
                val dao = withContext(Dispatchers.IO) { AppDatabase.getDatabase(context).poseMetadataDao() }
                val meta = withContext(Dispatchers.IO) { dao.getMetadataById(it.id) }
                it.isFavorite = meta?.isFavorite ?: false
                it.user_photo_url = meta?.customPhotoPath
            }

            todayPickPose.postValue(pose)
        }
    }

    fun toggleFavorite(context: Context, pose: YogaPoseModel) {
        viewModelScope.launch(Dispatchers.IO) {
            val poseDao = AppDatabase.getDatabase(context).poseMetadataDao()
            val currentMeta = poseDao.getMetadataById(pose.id)
            poseDao.insertOrUpdate(
                PoseMetadataEntity(
                    poseId = pose.id,
                    isFavorite = pose.isFavorite,
                    customPhotoPath = currentMeta?.customPhotoPath
                )
            )
        }
    }

    fun applyFilterAndSearch(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val poseDao = AppDatabase.getDatabase(context).poseMetadataDao()
            val localMeta = poseDao.getAllMetadata().associateBy { it.poseId }

            val filteredList = fullYogaList.filter { pose ->
                val isMatchCategory = currentCategory.equals("All", ignoreCase = true) ||
                        pose.category.equals(currentCategory, ignoreCase = true)
                val isMatchSearch = currentSearchQuery.isEmpty() ||
                        pose.name.contains(currentSearchQuery, ignoreCase = true)
                isMatchCategory && isMatchSearch
            }.map { pose ->
                val meta = localMeta[pose.id]
                pose.copy(
                    isFavorite = meta?.isFavorite ?: false,
                    user_photo_url = meta?.customPhotoPath
                )
            }

            withContext(Dispatchers.Main) {
                yogaPoseList.value = filteredList
            }
        }
    }
    fun updateTodayPickFromLocal(context: Context) {
        viewModelScope.launch {
            val currentPose = todayPickPose.value ?: return@launch
            val meta = withContext(Dispatchers.IO) {
                AppDatabase.getDatabase(context).poseMetadataDao().getMetadataById(currentPose.id)
            }
            currentPose.isFavorite = meta?.isFavorite ?: false
            currentPose.user_photo_url = meta?.customPhotoPath
            todayPickPose.value = currentPose
        }
    }
}
