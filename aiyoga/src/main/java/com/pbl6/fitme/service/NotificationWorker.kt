package com.hadat.aiyoga.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.hadat.aiyoga.MainActivity
import com.hadat.aiyoga.R
import java.util.Calendar
import java.util.concurrent.TimeUnit

class NotificationWorker(
    private val context: Context, workerParams: WorkerParameters
) : Worker(context, workerParams) {

    companion object {
        private const val CHANNEL_ID =
            "${AppFirebaseMessagingService.Companion.APP_PACKAGE_NAME}_daily_notifications"
        private const val CHANNEL_NAME = "Daily Reminders"
        private const val MORNING_NOTIFICATION_ID = 2
        private const val EVENING_NOTIFICATION_ID = 3

        fun scheduleDailyNotifications(context: Context, times: List<String>) {
            times.firstOrNull()?.let {
                val hour = it.substringBefore(":").toIntOrNull() ?: 19
                val minute = it.substringAfter(":").toIntOrNull() ?: 0
                scheduleMorningNotification(context, hour, minute)
            }
            times.getOrNull(1)?.let {
                val hour = it.substringBefore(":").toIntOrNull() ?: 19
                val minute = it.substringAfter(":").toIntOrNull() ?: 0
                scheduleEveningNotification(context, hour, minute)
            }
        }

        private fun scheduleMorningNotification(context: Context, hour: Int, minute: Int) {
            val morningWorkRequest = createDailyWorkRequest(context, hour, minute, "morning")
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "morning_notification", ExistingPeriodicWorkPolicy.UPDATE, morningWorkRequest
            )
        }

        private fun scheduleEveningNotification(context: Context, hour: Int, minute: Int) {
            val eveningWorkRequest = createDailyWorkRequest(context, hour, minute, "evening")
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "evening_notification", ExistingPeriodicWorkPolicy.UPDATE, eveningWorkRequest
            )
        }

        private fun createDailyWorkRequest(
            context: Context, hour: Int, minute: Int, tag: String
        ): PeriodicWorkRequest {
            // For Android 12 and below, notification permission is granted by default
            // For Android 13+, we only schedule if permission is granted
            if (Build.VERSION.SDK_INT >= 33) {
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if (!notificationManager.areNotificationsEnabled()) {
                    return PeriodicWorkRequestBuilder<NotificationWorker>(
                        24, TimeUnit.HOURS
                    ).build()
                }
            }
            val constraints =
                Constraints.Builder().setRequiredNetworkType(NetworkType.NOT_REQUIRED).build()

            val data = workDataOf("notification_type" to tag)

            return PeriodicWorkRequestBuilder<NotificationWorker>(
                24,
                TimeUnit.HOURS
            ).setConstraints(constraints).setInputData(data)
                .setInitialDelay(calculateInitialDelay(hour, minute), TimeUnit.MILLISECONDS).build()
        }

        private fun calculateInitialDelay(targetHour: Int, targetMinute: Int): Long {
            val currentTimeMillis = System.currentTimeMillis()
            val calendar = Calendar.getInstance().apply {
                timeInMillis = currentTimeMillis
                set(Calendar.HOUR_OF_DAY, targetHour)
                set(Calendar.MINUTE, targetMinute)
                set(Calendar.SECOND, 0)
            }

            if (calendar.timeInMillis <= currentTimeMillis) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }

            return calendar.timeInMillis - currentTimeMillis
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun doWork(): Result {
        // Check if app was opened today
        if (AppPreferences.wasAppOpenedToday(applicationContext)) {
            // Skip notification if app was already opened today
            return Result.success()
        }

        val messages =
            applicationContext.resources.getStringArray(R.array.daily_notification_messages)
        val randomMessage = messages.random()
        val notificationType = inputData.getString("notification_type") ?: return Result.failure()

        val (title, message, notificationId) = when (notificationType) {
            "morning" -> Triple(
                applicationContext.getString(R.string.app_name),
                randomMessage,
                MORNING_NOTIFICATION_ID
            )

            "evening" -> Triple(
                applicationContext.getString(R.string.app_name),
                randomMessage,
                EVENING_NOTIFICATION_ID
            )

            else -> return Result.failure()
        }

        showNotification(title, message, notificationId)
        return Result.success()
    }

    private fun showNotification(title: String, message: String, notificationId: Int) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android 8.0 (API 26) and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily reminders"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, notificationId, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification =
            NotificationCompat.Builder(context, CHANNEL_ID).setSmallIcon(R.drawable.image)
                .setContentTitle(title).setContentText(message).setAutoCancel(true)
                .setContentIntent(pendingIntent).setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setDefaults(NotificationCompat.DEFAULT_ALL) // Enable sound and vibration
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC).build()

        notificationManager.notify(notificationId, notification)
    }
}