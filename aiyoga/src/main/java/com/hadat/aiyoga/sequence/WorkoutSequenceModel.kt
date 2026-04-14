package com.hadat.aiyoga.sequence

import android.os.Parcelable
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
data class WorkoutSequenceModel(
    val userId: String = "",
    val title: String = "",
    val coverImageUrl: String = "",
    val totalDuration: String = "",
    val level: Int = 1,
    val poses: List<SequenceModel> = emptyList(),
    @ServerTimestamp
    val createdAt: Date? = null
) : Parcelable