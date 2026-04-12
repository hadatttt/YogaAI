package com.hadat.aiyoga.data.room

import androidx.room.*

@Dao
interface PoseMetadataDao {

    @Query("SELECT * FROM pose_metadata")
    suspend fun getAllMetadata(): List<PoseMetadataEntity>

    @Query("SELECT * FROM pose_metadata WHERE poseId = :id")
    suspend fun getMetadataById(id: Int): PoseMetadataEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(metadata: PoseMetadataEntity)

    @Query("UPDATE pose_metadata SET isFavorite = :isFav WHERE poseId = :id")
    suspend fun updateFavoriteStatus(id: Int, isFav: Boolean)

    @Delete
    suspend fun deleteMetadata(metadata: PoseMetadataEntity)
}