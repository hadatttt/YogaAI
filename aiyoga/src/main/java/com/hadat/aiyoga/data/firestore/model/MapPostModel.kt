package com.hadat.aiyoga.data.firestore.model

import android.os.Parcelable
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
data class MapPostModel(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userAvatar: String = "",
    val description: String = "",
    val imageUrls: List<String> = emptyList(),
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val placeName: String = "",
    val workoutId: String = "",
    val workoutTitle: String = "",
    @ServerTimestamp
    val createdAt: Date? = null
) : Parcelable

