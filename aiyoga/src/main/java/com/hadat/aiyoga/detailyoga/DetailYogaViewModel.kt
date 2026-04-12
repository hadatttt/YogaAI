package com.hadat.aiyoga.detailyoga

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.hadat.aiyoga.data.room.AppDatabase
import com.hadat.aiyoga.data.room.PoseMetadataEntity
import com.hadat.aiyoga.yogamain.YogaPoseModel
import hoang.dqm.codebase.base.viewmodel.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DetailYogaViewModel: BaseViewModel() {
    val isFavorite = MutableLiveData<Boolean>()
    val customPhotoPath = MutableLiveData<String?>()

    private fun getDao(context: Context) = AppDatabase.getDatabase(context).poseMetadataDao()

    fun checkFavoriteStatus(context: Context, poseId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val meta = getDao(context).getMetadataById(poseId)
            withContext(Dispatchers.Main) {
                isFavorite.value = meta?.isFavorite ?: false
                customPhotoPath.value = meta?.customPhotoPath
            }
        }
    }

    fun toggleFavorite(context: Context, pose: YogaPoseModel) {
        viewModelScope.launch(Dispatchers.IO) {
            val dao = getDao(context)
            val currentMeta = dao.getMetadataById(pose.id)
            val newStatus = !(isFavorite.value ?: false)

            dao.insertOrUpdate(
                PoseMetadataEntity(
                    poseId = pose.id,
                    isFavorite = newStatus,
                    customPhotoPath = customPhotoPath.value ?: currentMeta?.customPhotoPath
                )
            )

            withContext(Dispatchers.Main) {
                isFavorite.value = newStatus
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
                    isFavorite = isFavorite.value ?: currentMeta?.isFavorite ?: false,
                    customPhotoPath = path
                )
            )
            withContext(Dispatchers.Main) {
                customPhotoPath.value = path
            }
        }
    }
}