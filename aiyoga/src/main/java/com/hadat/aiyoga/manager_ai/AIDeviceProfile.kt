package com.hadat.aiyoga.manager_ai

import android.app.ActivityManager
import android.content.Context

enum class AIPerformanceLevel {
    LOW,
    MEDIUM,
    HIGH
}

data class AIConfig(
    val level: AIPerformanceLevel,
    val classifierThreads: Int,
    val classifierIntervalMs: Long
)

object AIDeviceProfile {
    fun defaultConfig() = AIConfig(
        level = AIPerformanceLevel.MEDIUM,
        classifierThreads = 3,
        classifierIntervalMs = 500L
    )

    fun detect(context: Context): AIConfig {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        val totalRamGb = memoryInfo.totalMem / 1024f / 1024f / 1024f
        val cpuCores = Runtime.getRuntime().availableProcessors()

        val level = when {
            activityManager.isLowRamDevice || totalRamGb < 4f || cpuCores <= 4 -> AIPerformanceLevel.LOW
            totalRamGb >= 7f && cpuCores >= 8 -> AIPerformanceLevel.HIGH
            else -> AIPerformanceLevel.MEDIUM
        }

        val config = when (level) {
            AIPerformanceLevel.LOW -> AIConfig(level, classifierThreads = 2, classifierIntervalMs = 1000L)
            AIPerformanceLevel.MEDIUM -> AIConfig(level, classifierThreads = 3, classifierIntervalMs = 500L)
            AIPerformanceLevel.HIGH -> AIConfig(level, classifierThreads = 4, classifierIntervalMs = 500L)
        }

        return config
    }
}
