package com.hadat.aiyoga.yogamain

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class YogaPoseModel(
    val id: Int,
    val name: String,
    val photo_url: String,
    val category: String,
    val expertise_level: Int
) : Parcelable