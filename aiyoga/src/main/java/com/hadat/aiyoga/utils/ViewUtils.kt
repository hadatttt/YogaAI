package com.hadat.aiyoga.utils

import android.content.Context
import android.util.DisplayMetrics
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView

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
}