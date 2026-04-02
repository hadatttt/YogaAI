package com.example.dailycheckin.model.entity

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity
@Keep
data class CheckInDay(
    @PrimaryKey
    var id:Int,
    var date: LocalDate,
    var isChecked: Boolean
) {
}