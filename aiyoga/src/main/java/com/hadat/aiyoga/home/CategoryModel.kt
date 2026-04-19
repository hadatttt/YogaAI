package com.hadat.aiyoga.home

data class CategoryModel(
    val id: Int,
    val value: String,
    val displayValue: String = value
)