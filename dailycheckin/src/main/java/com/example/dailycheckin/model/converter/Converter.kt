package com.example.dailycheckin.model.converter

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.room.TypeConverter
import com.google.gson.Gson
import java.time.LocalDate

class Converter {
    private val gson = Gson()
    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? {
        return date?.toString() // ISO-8601 format: "2025-07-23"
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @TypeConverter
    fun toLocalDate(dateString: String?): LocalDate? {
        return dateString?.let { LocalDate.parse(it) }
    }
}