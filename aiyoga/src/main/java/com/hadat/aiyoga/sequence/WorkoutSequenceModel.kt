package com.hadat.aiyoga.sequence

import android.os.Parcelable
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
data class WorkoutSequenceModel(
    val id: String = "",
    var userId: String = "",
    var title: String = "",
    var authorName: String = "",
    var coverImageUrl: String = "",
    var totalDuration: String = "",
    var level: Int = 1,
    var isPublic: Boolean = true,
    var poses: List<SequenceModel> = emptyList(),
    var likeCount: Int = 0,
    var viewCount: Int = 0,
    @ServerTimestamp
    var createdAt: Date? = null
) : Parcelable