package com.hadat.aiyoga.data.firestore.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class HealthProfileModel(
    val userId: String = "",
    val gender: String = "",
    val age: Int = 0,
    val weight: Float = 0f,
    val height: Float = 0f,
    val activityLevel: Int = 0,
    val bmi: Float = 0f,
    val bmr: Float = 0f,
    val tdee: Float = 0f,
) : Parcelable