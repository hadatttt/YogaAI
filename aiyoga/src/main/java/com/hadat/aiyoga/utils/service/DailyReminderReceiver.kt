package com.hadat.aiyoga.utils.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.hadat.aiyoga.MainActivity
import com.hadat.aiyoga.R

class DailyReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED -> {
                val reminderTime = AppPreferences.getNotificationTime(context)
                NotificationWorker.scheduleDailyNotifications(context, listOf(reminderTime))
            }

            ACTION_DAILY_REMINDER -> {
                NotificationWorker.rescheduleFromIntent(context, intent)
                showReminderNotification(context, intent)
            }
        }
    }

    private fun showReminderNotification(context: Context, intent: Intent) {
        val notificationType = intent.getStringExtra(EXTRA_NOTIFICATION_TYPE) ?: "morning"
        val notificationId = if (notificationType == "evening") EVENING_NOTIFICATION_ID else MORNING_NOTIFICATION_ID
        val messages = context.resources.getStringArray(R.array.daily_notification_messages)
        val message = messages.random()
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily reminders"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.image)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    companion object {
        const val ACTION_DAILY_REMINDER = "com.hadat.aiyoga.DAILY_REMINDER"
        const val EXTRA_NOTIFICATION_TYPE = "notification_type"
        const val EXTRA_HOUR = "hour"
        const val EXTRA_MINUTE = "minute"

        private const val CHANNEL_ID = "com.hadat.aiyoga_daily_notifications"
        private const val CHANNEL_NAME = "Daily Reminders"
        private const val MORNING_NOTIFICATION_ID = 2
        private const val EVENING_NOTIFICATION_ID = 3
    }
}
