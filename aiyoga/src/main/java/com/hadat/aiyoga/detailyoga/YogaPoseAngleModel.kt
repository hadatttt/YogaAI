package com.hadat.aiyoga.detailyoga
import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class YogaPoseAngleModel(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("knee_L") val knee_L: Double,
    @SerializedName("knee_R") val knee_R: Double,
    @SerializedName("hip_L") val hip_L: Double,
    @SerializedName("hip_R") val hip_R: Double,
    @SerializedName("arm_body_L") val arm_body_L: Double,
    @SerializedName("arm_body_R") val arm_body_R: Double,
    @SerializedName("elbow_L") val elbow_L: Double,
    @SerializedName("elbow_R") val elbow_R: Double
) : Parcelable