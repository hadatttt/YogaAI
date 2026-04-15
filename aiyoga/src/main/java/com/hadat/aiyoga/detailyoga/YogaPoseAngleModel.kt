package com.hadat.aiyoga.detailyoga


import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class YogaPoseAngleModel(
    val id: Int,
    val name: String,
    val knee_L: Double,
    val knee_R: Double,
    val hip_L: Double,
    val hip_R: Double,
    val arm_body_L: Double,
    val arm_body_R: Double,
    val elbow_L: Double,
    val elbow_R: Double
) : Parcelable