package com.hadat.aiyoga.utils.service

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.edit
import java.time.LocalDate
import java.util.Locale

object AppPreferences {
    private const val PREF_NAME = "yoga_prefs"
    private const val KEY_LAST_APP_OPEN_DATE = "last_app_open_date"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_NOTIFICATION_TIME = "notification_time"
    private const val KEY_LANGUAGE_CODE = "language_code"
    private const val KEY_IS_HEALTH_PROFILE_COMPLETED = "is_health_profile_completed"
    fun setHealthProfileCompleted(context: Context, isCompleted: Boolean) {
        getPrefs(context).edit {
            putBoolean(KEY_IS_HEALTH_PROFILE_COMPLETED, isCompleted)
        }
    }
    fun isHealthProfileCompleted(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_IS_HEALTH_PROFILE_COMPLETED, false)
    }
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

    fun setLoginStatus(context: Context, isLoggedIn: Boolean, userId: String? = null) {
        getPrefs(context).edit {
            putBoolean(KEY_IS_LOGGED_IN, isLoggedIn)
            if (userId != null) putString(KEY_USER_ID, userId)
        }
    }


    fun isLoggedIn(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun getUserId(context: Context): String? {
        return getPrefs(context).getString(KEY_USER_ID, null)
    }

    fun logout(context: Context) {
        getPrefs(context).edit(commit = true) {
            putBoolean(KEY_IS_LOGGED_IN, false)
            remove(KEY_USER_ID)
            putBoolean(KEY_IS_HEALTH_PROFILE_COMPLETED, false)
        }
    }

    fun setNotificationTime(context: Context, time: String) {
        getPrefs(context).edit { putString(KEY_NOTIFICATION_TIME, time) }
    }

    fun getNotificationTime(context: Context): String {
        return getPrefs(context).getString(KEY_NOTIFICATION_TIME, "19:00") ?: "19:00"
    }

    fun setLanguageCode(context: Context, languageCode: String) {
        getPrefs(context).edit { putString(KEY_LANGUAGE_CODE, languageCode) }
    }

    fun getLanguageCode(context: Context): String {
        val prefs = getPrefs(context)
        if (!prefs.contains(KEY_LANGUAGE_CODE)) {
            val systemLang = Locale.getDefault().language
            val defaultLang = if (systemLang == "vi") "vi" else "en"
            setLanguageCode(context, defaultLang)
            return defaultLang
        }
        return prefs.getString(KEY_LANGUAGE_CODE, "en") ?: "en"
    }
}