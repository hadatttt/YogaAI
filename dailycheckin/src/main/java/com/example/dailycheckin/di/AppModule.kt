package com.example.dailycheckin.di

import android.content.Context
import androidx.room.Room
import com.example.dailycheckin.model.dao.AppDatabase
import com.example.dailycheckin.model.dao.CheckInDao

object AppModule {
    @Volatile
    private var _instance: AppDatabase? = null

    fun getDB(context: Context): AppDatabase{
        return _instance?: synchronized(this){
            _instance?: run {
                val database = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lib_checkIn"
                ).build()
                _instance = database
                database
            }
        }
    }

    fun getCheckInDao(context: Context): CheckInDao{
        return getDB(context).checkInDao()
    }
}