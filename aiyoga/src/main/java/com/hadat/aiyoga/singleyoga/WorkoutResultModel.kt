package com.hadat.aiyoga.singleyoga

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class WorkoutResultModel(
    val userId: String = "",
    val poseId: Int = 0,
    val poseUrl: String = "",
    val poseName: String = "",
    val durationInSeconds: Int = 0,
    val date: String = "",
    val capturedImages: List<String> = emptyList(),
    val errorCount: Int = 0,
    val workoutTimestamp: Long = 0L
) : Parcelable