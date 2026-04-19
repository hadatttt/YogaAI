package com.hadat.aiyoga.sequence

import android.os.Parcelable
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
data class WorkoutSequenceModel(
    var id: String = "",
    var userId: String = "",
    var title: String = "",
    var authorName: String = "",
    var coverImageUrl: String = "",
    var totalDuration: String = "",
    var level: Int = 1,

    @get:PropertyName("isPublic")
    @set:PropertyName("isPublic")
    var isPublic: Boolean = false,

    var poses: List<SequenceModel> = emptyList(),
    var likeCount: Int = 0,
    var viewCount: Int = 0,

    @ServerTimestamp
    var createdAt: Date? = null
) : Parcelable {
    constructor() : this("")
}