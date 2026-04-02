package com.example.dailycheckin.model.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.dailycheckin.model.entity.CheckInDay

@Dao
interface CheckInDao {
    @Query("Select * from CheckInDay")
    suspend fun getAll(): List<CheckInDay>

    @Update
    suspend fun checkIn(checkedInDay: CheckInDay)

    @Insert
    suspend fun addListCheckIn(list: List<CheckInDay>)

    @Query("Delete from checkinday")
    suspend fun deleteAll()
}