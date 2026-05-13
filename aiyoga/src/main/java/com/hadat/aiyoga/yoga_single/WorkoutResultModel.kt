package com.hadat.aiyoga.yoga_single

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class WorkoutResultModel(
    val userId: String = "",
    val poseId: Int = 0,
    val poseUrl: String = "",
    val poseName: String = "",
    var durationInSeconds: Int = 0,
    val date: String = "",
    var errorCount: Int = 0,
    val workoutTimestamp: Long = 0L
) : Parcelable