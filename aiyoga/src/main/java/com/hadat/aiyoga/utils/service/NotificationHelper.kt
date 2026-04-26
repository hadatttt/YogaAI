package com.hadat.aiyoga.utils.service

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.Fragment
import com.hadat.aiyoga.MainActivity
import com.hadat.aiyoga.R

object NotificationHelper {

    @RequiresApi(Build.VERSION_CODES.O)
    fun testNotification(context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        // Create notification channel for Android 8.0 (API 26) and above
        val channel = NotificationChannel(
            "test_channel", "Test Channel", NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Channel for testing notifications"
            enableLights(true)
            enableVibration(true)
        }
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(context, "test_channel")
            .setSmallIcon(R.drawable.image).setContentTitle("Test Notification")
            .setContentText("This is a test notification from Pixel Art").setAutoCancel(true)
            .setContentIntent(pendingIntent).setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setDefaults(NotificationCompat.DEFAULT_ALL).build()

        notificationManager.notify(100, notification)
    }

    fun checkPermission(
        fragment: Fragment,
        onGranted: () -> Unit,
        permissionLauncher: ActivityResultLauncher<String>
    ) {
        // Only request notification permission on Android 13 and above
        if (Build.VERSION.SDK_INT >= 33) {
            val notificationManager = NotificationManagerCompat.from(fragment.requireContext())
            if (notificationManager.areNotificationsEnabled()) {
                // Notifications are already enabled
                onGranted()
            } else if (fragment.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                showRationaleDialog(fragment.requireActivity(), permissionLauncher)
            } else {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            // For Android 12 and below, notification permission is granted by default
            onGranted()
        }
    }

    private fun showRationaleDialog(
        activity: Activity, permissionLauncher: ActivityResultLauncher<String>
    ) {
        if (Build.VERSION.SDK_INT >= 33) {
            AlertDialog.Builder(activity).setTitle(R.string.notification_permission_title)
                .setMessage(R.string.notification_permission_message)
                .setPositiveButton(R.string.i_got_it_text) { _, _ ->
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }.setNegativeButton(R.string.title_cancel, null).show()
        }
    }

    fun showSettingsDialog(activity: Activity) {
        AlertDialog.Builder(activity).setTitle(R.string.notification_permission_title)
            .setMessage(R.string.notification_permission_denied)
            .setPositiveButton(R.string.i_got_it_text) { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.fromParts("package", activity.packageName, null)
                activity.startActivity(intent)
            }.setNegativeButton(R.string.title_cancel, null).show()
    }
}