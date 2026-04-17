package com.hadat.aiyoga.detailyoga

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.hadat.aiyoga.data.room.AppDatabase
import com.hadat.aiyoga.data.room.PoseMetadataEntity
import com.hadat.aiyoga.utils.yogautils.YogaDataUtils
import com.hadat.aiyoga.yogamain.YogaPoseModel
import hoang.dqm.codebase.base.viewmodel.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DetailYogaViewModel : BaseViewModel() {

    private val _isFavorite = MutableLiveData<Boolean>()
    val isFavorite: LiveData<Boolean> get() = _isFavorite

    private val _customPhotoPath = MutableLiveData<String?>()
    val customPhotoPath: LiveData<String?> get() = _customPhotoPath

    private val _yogaDetail = MutableLiveData<YogaPoseDetailModel?>()
    val yogaDetail: LiveData<YogaPoseDetailModel?> get() = _yogaDetail

    private val _selectedCategoryId = MutableLiveData(0)
    val selectedCategoryId: LiveData<Int> get() = _selectedCategoryId

    private fun getDao(context: Context) = AppDatabase.getDatabase(context).poseMetadataDao()

    fun checkFavoriteStatus(context: Context, poseId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val meta = getDao(context).getMetadataById(poseId)
            withContext(Dispatchers.Main) {
                _isFavorite.value = meta?.isFavorite ?: false
                _customPhotoPath.value = meta?.customPhotoPath
            }
        }
    }

    fun fetchYogaDetail(poseId: Int) {
        if (_yogaDetail.value?.id == poseId) return

        YogaDataUtils.getRemoteYogaDetail(poseId) { detail ->
            _yogaDetail.postValue(detail)
        }
    }

    fun selectCategory(id: Int) {
        _selectedCategoryId.value = id
    }

    fun toggleFavorite(context: Context, pose: YogaPoseModel) {
        viewModelScope.launch(Dispatchers.IO) {
            val dao = getDao(context)
            val currentMeta = dao.getMetadataById(pose.id)
            val newStatus = !(_isFavorite.value ?: false)

            dao.insertOrUpdate(
                PoseMetadataEntity(
                    poseId = pose.id,
                    isFavorite = newStatus,
                    customPhotoPath = _customPhotoPath.value ?: currentMeta?.customPhotoPath
                )
            )

            withContext(Dispatchers.Main) {
                _isFavorite.value = newStatus
            }
        }
    }

    fun updateCustomPhoto(context: Context, poseId: Int, path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val dao = getDao(context)
            val currentMeta = dao.getMetadataById(poseId)

            dao.insertOrUpdate(
                PoseMetadataEntity(
                    poseId = poseId,
                    isFavorite = _isFavorite.value ?: currentMeta?.isFavorite ?: false,
                    customPhotoPath = path
                )
            )
            withContext(Dispatchers.Main) {
                _customPhotoPath.value = path
            }
        }
    }
}