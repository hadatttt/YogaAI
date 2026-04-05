package com.hadat.aiyoga.service

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.edit
import java.time.LocalDate

object AppPreferences {
    private const val PREF_NAME = "tap_puzzle_gallery_prefs"
    private const val KEY_LAST_APP_OPEN_DATE = "last_app_open_date"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun updateLastAppOpenDate(context: Context) {
        val today = LocalDate.now().toString()
        getPrefs(context).edit { putString(KEY_LAST_APP_OPEN_DATE, today) }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getLastAppOpenDate(context: Context): LocalDate? {
        val dateStr = getPrefs(context).getString(KEY_LAST_APP_OPEN_DATE, null)
        return dateStr?.let { LocalDate.parse(it) }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun wasAppOpenedToday(context: Context): Boolean {
        val lastOpenDate = getLastAppOpenDate(context)
        return lastOpenDate == LocalDate.now()
    }
}