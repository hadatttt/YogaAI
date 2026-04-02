package com.example.dailycheckin.utils

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.dailycheckin.di.AppModule
import com.example.dailycheckin.model.entity.CheckInDay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object DailyCheckIn {
    private var currentCheckIn: List<CheckInDay>? = null
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getListCheckIn(context: Context): List<CheckInDay> {
        if (currentCheckIn == null) {
            val checkedInDao = AppModule.getCheckInDao(context)
            val currentEpoch = System.currentTimeMillis()
            val zoneId = ZoneId.systemDefault()
            val today = Instant.ofEpochMilli(currentEpoch).atZone(zoneId).toLocalDate()

            var checkInHistory = withContext(Dispatchers.IO) {
                checkedInDao.getAll()
            }

            if (checkInHistory.isNotEmpty() && checkInHistory[checkInHistory.lastIndex].date < today){
                checkedInDao.deleteAll()
                checkInHistory = emptyList()
            }

            if (checkInHistory.isEmpty()) {
                val listCheckIn = mutableListOf<CheckInDay>()
                for (i in 0..29) {
                    listCheckIn.add(CheckInDay(id = i, date = today.plusDays(i.toLong()), false))
                }
                withContext(Dispatchers.IO) {
                    checkedInDao.addListCheckIn(listCheckIn)
                }
                currentCheckIn = listCheckIn
            } else {
                currentCheckIn = checkInHistory
            }
        }
        return currentCheckIn as List<CheckInDay>
    }

    suspend fun checkIn(context: Context, today: LocalDate): Boolean {
        val checkInDay = currentCheckIn?.find { it.date.equals(today) }
        if (checkInDay?.isChecked == true) return true
        else {
            checkInDay?.isChecked = true
            checkInDay?.let {
                withContext(Dispatchers.IO) {
                    AppModule.getCheckInDao(context).checkIn(it)
                }
                return true
            }
            return false
        }
    }
}