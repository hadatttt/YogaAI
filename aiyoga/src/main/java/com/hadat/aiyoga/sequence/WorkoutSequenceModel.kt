package com.hadat.aiyoga.sequence

import android.os.Parcelable
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
data class WorkoutSequenceModel(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val coverImageUrl: String = "",
    val totalDuration: String = "",
    val level: Int = 1,
    val isPublic: Boolean = true,
    val poses: List<SequenceModel> = emptyList(),
    val likeCount: Int = 0,
    val viewCount: Int = 0,
    @ServerTimestamp
    val createdAt: Date? = null
) : Parcelable