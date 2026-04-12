package com.hadat.aiyoga.yogamain

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class YogaPoseModel(
    val id: Int,
    val name: String,
    val photo_url: String,
    val category: String,
    val expertise_level: Int,
    var isFavorite: Boolean = false,
    var user_photo_url: String? = null
) : Parcelable{
fun getDisplayPhoto(): String {
    return if (!user_photo_url.isNullOrEmpty()) user_photo_url!! else photo_url
}
}