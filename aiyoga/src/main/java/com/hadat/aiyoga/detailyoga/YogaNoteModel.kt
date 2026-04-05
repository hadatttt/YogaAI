package com.hadat.aiyoga.detailyoga

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class YogaNoteModel(
    val title: String,
    val description: String
) : Parcelable