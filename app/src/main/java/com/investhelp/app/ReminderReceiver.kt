package com.investhelp.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

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

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Reminder: $ticker")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(itemId.toInt(), notification)
    }
}
