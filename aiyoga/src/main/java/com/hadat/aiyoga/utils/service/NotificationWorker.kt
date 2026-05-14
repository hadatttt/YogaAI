package com.hadat.aiyoga.utils.service

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.hadat.aiyoga.MainActivity
import com.hadat.aiyoga.R
import java.util.Calendar

class NotificationWorker(
    private val context: Context, workerParams: WorkerParameters
) : Worker(context, workerParams) {

    companion object {
        private const val CHANNEL_ID =
            "${AppFirebaseMessagingService.Companion.APP_PACKAGE_NAME}_daily_notifications"
        private const val CHANNEL_NAME = "Daily Reminders"
        private const val MORNING_NOTIFICATION_ID = 2
        private const val EVENING_NOTIFICATION_ID = 3
        private const val MORNING_REQUEST_CODE = 2002
        private const val EVENING_REQUEST_CODE = 2003

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
            scheduleExactDailyAlarm(context, hour, minute, "morning", MORNING_REQUEST_CODE)
        }

        private fun scheduleEveningNotification(context: Context, hour: Int, minute: Int) {
            scheduleExactDailyAlarm(context, hour, minute, "evening", EVENING_REQUEST_CODE)
        }

        fun scheduleExactDailyAlarm(
            context: Context,
            hour: Int,
            minute: Int,
            notificationType: String,
            requestCode: Int
        ) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val pendingIntent = createReminderPendingIntent(context, notificationType, hour, minute, requestCode)
            val triggerAtMillis = calculateNextTriggerTime(hour, minute)

            alarmManager.cancel(pendingIntent)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                return
            }

            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }

        fun rescheduleFromIntent(context: Context, intent: Intent) {
            val notificationType = intent.getStringExtra(DailyReminderReceiver.EXTRA_NOTIFICATION_TYPE) ?: "morning"
            val hour = intent.getIntExtra(DailyReminderReceiver.EXTRA_HOUR, 19)
            val minute = intent.getIntExtra(DailyReminderReceiver.EXTRA_MINUTE, 0)
            val requestCode = if (notificationType == "evening") EVENING_REQUEST_CODE else MORNING_REQUEST_CODE
            scheduleExactDailyAlarm(context, hour, minute, notificationType, requestCode)
        }

        private fun createReminderPendingIntent(
            context: Context,
            notificationType: String,
            hour: Int,
            minute: Int,
            requestCode: Int
        ): PendingIntent {
            val intent = Intent(context, DailyReminderReceiver::class.java).apply {
                action = DailyReminderReceiver.ACTION_DAILY_REMINDER
                putExtra(DailyReminderReceiver.EXTRA_NOTIFICATION_TYPE, notificationType)
                putExtra(DailyReminderReceiver.EXTRA_HOUR, hour)
                putExtra(DailyReminderReceiver.EXTRA_MINUTE, minute)
            }
            return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun calculateNextTriggerTime(targetHour: Int, targetMinute: Int): Long {
            val currentTimeMillis = System.currentTimeMillis()
            val calendar = Calendar.getInstance().apply {
                timeInMillis = currentTimeMillis
                set(Calendar.HOUR_OF_DAY, targetHour)
                set(Calendar.MINUTE, targetMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            if (calendar.timeInMillis <= currentTimeMillis) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }

            return calendar.timeInMillis
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
