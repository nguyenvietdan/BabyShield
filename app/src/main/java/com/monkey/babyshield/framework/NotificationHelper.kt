package com.monkey.babyshield.framework

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.monkey.babyshield.R
import com.monkey.babyshield.presentations.MainActivity


object NotificationHelper {

    const val NOTIFICATION_ID = 1
    private const val CHANNEL_ID = "TouchBlockerChannel"
    private const val NOTIFICATION_NAME = "Touch Blocker Channel"
    private const val NOTIFICATION_DESCRIPTION = "Touch Blocker Service Channel"

    fun createNotificationChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            NOTIFICATION_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = NOTIFICATION_DESCRIPTION
        }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    fun createNotification(context: Context): Notification {
        val notificationIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(context.getString(R.string.content_title))
            .setContentText(context.getString(R.string.content_text))
            .setSmallIcon(R.drawable.ic_lock)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .build()
    }

}