package com.allineami.app.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Reminders every 15 minutes, up to 8 hours, while the timer is running. */
object Reminders {
    private val intervals = (15..8 * 60 step 15).toList()
    const val EXTRA_MINUTES = "minutes"
    private const val REQUEST_CODE_BASE = 0x524C

    // If false, Android may delay reminders by 10-20 minutes
    private val _exactAllowed = MutableStateFlow(true)
    val exactAllowed: StateFlow<Boolean> = _exactAllowed.asStateFlow()

    fun refreshExactAllowed(context: Context): Boolean {
        val am = context.getSystemService(AlarmManager::class.java)
        val allowed = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()
        _exactAllowed.value = allowed
        return allowed
    }

    fun schedule(context: Context, start: Long) {
        val am = context.getSystemService(AlarmManager::class.java)
        val exact = refreshExactAllowed(context)
        val now = System.currentTimeMillis()
        for (minutes in intervals) {
            val fireAt = start + minutes * 60_000L
            if (fireAt <= now) continue
            val pi = pendingIntent(context, minutes)
            if (exact) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fireAt, pi)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fireAt, pi)
            }
        }
    }

    fun cancel(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java)
        for (minutes in intervals) {
            am.cancel(pendingIntent(context, minutes))
        }
    }

    private fun pendingIntent(context: Context, minutes: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_BASE + minutes,
            Intent(context, ReminderReceiver::class.java).putExtra(EXTRA_MINUTES, minutes),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
}
