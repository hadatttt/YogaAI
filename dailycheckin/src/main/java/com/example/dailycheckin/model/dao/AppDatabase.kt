package com.example.dailycheckin.model.dao

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.dailycheckin.model.converter.Converter
import com.example.dailycheckin.model.entity.CheckInDay

@Database(entities = [CheckInDay::class], version = 1)
@TypeConverters(value = [Converter::class])
abstract class AppDatabase: RoomDatabase() {
    abstract fun checkInDao(): CheckInDao
}