package com.hadat.aiyoga.detailyoga

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class StepModel(
    val step_number: Int,
    val title: String,
    val description: String,
) : Parcelable