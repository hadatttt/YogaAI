package com.hadat.aiyoga.data.firestore.model

import android.os.Parcelable
import com.google.firebase.firestore.DocumentId
import kotlinx.parcelize.Parcelize

@Parcelize
data class LikeModel(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val sequenceId: String = ""
) : Parcelable