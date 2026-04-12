package com.hadat.aiyoga.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pose_metadata")
data class PoseMetadataEntity(
    @PrimaryKey val poseId: Int,
    val customPhotoPath: String? = null,
    val isFavorite: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
)