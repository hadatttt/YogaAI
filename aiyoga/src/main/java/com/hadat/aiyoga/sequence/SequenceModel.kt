package com.hadat.aiyoga.sequence

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SequenceModel(
    val id: Int = 0,
    var name: String = "",
    var category: String = "",
    var duration: String = "01:00",
    var photoUrl: String = ""
) : Parcelable