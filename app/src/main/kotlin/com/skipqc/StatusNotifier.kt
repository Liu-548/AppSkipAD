package com.skipqc

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

// R-20: a silent, ongoing status icon while the app is doing its job.
object StatusNotifier {

    private const val CHANNEL = "status"
    private const val ID = 1

    fun update(context: Context, active: Boolean) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (!active) {
            manager.cancel(ID)
            return
        }
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL,
                context.getString(R.string.channel_status),
                NotificationManager.IMPORTANCE_LOW,
            )
        )
        manager.notify(ID, build(context))
    }

    private fun build(context: Context): Notification =
        Notification.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_logo)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(R.string.status_active))
            .setOngoing(true)
            .setShowWhen(false)
            .setContentIntent(
                PendingIntent.getActivity(
                    context, 0, Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE,
                )
            )
            .addAction(
                Notification.Action.Builder(
                    null,
                    context.getString(R.string.turn_off),
                    PendingIntent.getBroadcast(
                        context, 0, Intent(context, TurnOffReceiver::class.java),
                        PendingIntent.FLAG_IMMUTABLE,
                    ),
                ).build()
            )
            .build()
}

/** The notification's "Tắt" action. R-14: it goes through Prefs like every other switch. */
class TurnOffReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Prefs.get(context).setActive(false)
    }
}
