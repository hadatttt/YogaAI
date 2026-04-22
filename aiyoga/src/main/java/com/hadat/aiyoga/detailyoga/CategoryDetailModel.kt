package com.hadat.aiyoga.detailyoga

import androidx.annotation.StringRes

data class CategoryDetailModel(
    val id: Int,
    @StringRes val titleRes: Int,
    val iconRes: Int
)