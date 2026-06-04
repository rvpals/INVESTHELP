package com.investhelp.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

class ReminderReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "watch_list_reminders"
        const val EXTRA_TICKER = "ticker"
        const val EXTRA_MESSAGE = "message"
        const val EXTRA_ITEM_ID = "item_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val ticker = intent.getStringExtra(EXTRA_TICKER) ?: return
        val message = intent.getStringExtra(EXTRA_MESSAGE) ?: ""
        val itemId = intent.getLongExtra(EXTRA_ITEM_ID, 0)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Watch List Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Reminders for watch list tickers"
        }
        notificationManager.createNotificationChannel(channel)

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_TICKER, ticker)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            itemId.toInt(),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Reminder: $ticker")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(itemId.toInt(), notification)
    }
}
