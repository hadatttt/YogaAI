package com.hadat.aiyoga.utils

import android.content.Context
import android.util.DisplayMetrics
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.hadat.aiyoga.R

object ViewUtils {

    fun scrollToCenter(
        recyclerView: RecyclerView,
        position: Int,
        millisecondsPerInch: Float = 1000f
    ) {
        val layoutManager = recyclerView.layoutManager ?: return

        val smoothScroller = object : LinearSmoothScroller(recyclerView.context) {
            override fun getHorizontalSnapPreference(): Int = SNAP_TO_ANY

            override fun calculateDtToFit(
                viewStart: Int,
                viewEnd: Int,
                boxStart: Int,
                boxEnd: Int,
                snapPreference: Int
            ): Int {
                return (boxStart + (boxEnd - boxStart) / 2) - (viewStart + (viewEnd - viewStart) / 2)
            }

            override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics): Float {
                return millisecondsPerInch / displayMetrics.densityDpi
            }
        }

        smoothScroller.targetPosition = position
        layoutManager.startSmoothScroll(smoothScroller)
    }
    fun String.removeVietnameseAccents(): String {
        var temp = java.text.Normalizer.normalize(this, java.text.Normalizer.Form.NFD)
        temp = Regex("\\p{InCombiningDiacriticalMarks}+").replace(temp, "")
        temp = temp.replace("đ", "d").replace("Đ", "D")
        temp = temp.replace(Regex("[^\\p{Alnum} ]"), "")
        return temp.trim()
    }
    fun getGreeting(context: Context): String {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..11 -> context.getString(R.string.greeting_morning)
            in 12..16 -> context.getString(R.string.greeting_afternoon)
            in 17..20 -> context.getString(R.string.greeting_evening)
            else -> context.getString(R.string.greeting_night)
        }
    }
}