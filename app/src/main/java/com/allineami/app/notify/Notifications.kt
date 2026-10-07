package com.allineami.app.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.allineami.app.MainActivity
import com.allineami.app.R

/**
 * Ongoing chronometer notification (the equivalent of the iOS Live Activity) and reminders.
 * The texts are deliberately generic: they are visible on the lock screen too.
 */
object Notifications {
    private const val CHANNEL_TIMER = "timer"
    private const val CHANNEL_REMINDERS = "reminders"
    private const val ID_ONGOING = 1
    private const val ID_REMINDER = 2

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_TIMER, "Timer attivo", NotificationManager.IMPORTANCE_LOW)
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_REMINDERS, "Promemoria", NotificationManager.IMPORTANCE_DEFAULT)
        )
    }

    fun showOngoing(context: Context, start: Long) {
        val notification = NotificationCompat.Builder(context, CHANNEL_TIMER)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle("Timer attivo")
            .setContentText("Ricordati di fermare il timer!")
            .setWhen(start)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openAppIntent(context))
            .build()
        notify(context, ID_ONGOING, notification)
    }

    /** After midnight the saved start changes: if the notification is already shown, keep the original chronometer. */
    fun showOngoingIfMissing(context: Context, start: Long) {
        val nm = context.getSystemService(NotificationManager::class.java)
        if (nm.activeNotifications.none { it.id == ID_ONGOING }) showOngoing(context, start)
    }

    fun cancelOngoing(context: Context) {
        NotificationManagerCompat.from(context).cancel(ID_ONGOING)
        NotificationManagerCompat.from(context).cancel(ID_REMINDER)
    }

    fun showReminder(context: Context, minutes: Int) {
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle("Timer ancora attivo ⏱️")
            .setContentText("Il timer è attivo da ${minuteLabel(minutes)}.")
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(context))
            .build()
        notify(context, ID_REMINDER, notification)
    }

    private fun notify(context: Context, id: Int, notification: android.app.Notification) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return
        NotificationManagerCompat.from(context).notify(id, notification)
    }

    private fun openAppIntent(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun minuteLabel(minutes: Int): String {
        if (minutes < 60) return "$minutes minuti"
        val h = minutes / 60
        val m = minutes % 60
        return if (m == 0) "$h ${if (h == 1) "ora" else "ore"}" else "${h}h ${m}min"
    }
}
