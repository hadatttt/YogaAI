package com.hadat.aiyoga.detailyoga

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class YogaPoseDetailModel(
    val id: Int,
    val description: String,
    val benefits: List<YogaNoteModel>,
    val contraindications: List<YogaNoteModel>,
    val steps: List<StepModel>
) : Parcelable