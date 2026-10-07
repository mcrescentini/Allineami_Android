package com.allineami.app.data

import android.content.Context
import com.allineami.app.notify.Notifications
import com.allineami.app.notify.Reminders

/**
 * Session start/stop logic, shared by the UI and the receivers.
 * Every day settles its accounts at midnight: whatever is left over never carries into the next day.
 */
object Tracker {

    fun start(context: Context) {
        val start = System.currentTimeMillis()
        Store.setRunningStart(start)
        Reminders.schedule(context, start)
        Notifications.showOngoing(context, start)
    }

    fun stop(context: Context) {
        val now = System.currentTimeMillis()
        checkMidnightCrossing(now)
        val start = Store.runningStart.value ?: return
        val effectiveStart = maxOf(start, DateUtils.startOfDay(now))
        // If the clock was moved back the duration would be negative: save 0
        val seconds = maxOf(0, ((now - effectiveStart) / 1000).toInt())
        Store.addSession(now, seconds, DateUtils.dayKey(now))
        Store.setRunningStart(null)

        Reminders.cancel(context)
        Notifications.cancelOngoing(context)
    }

    /**
     * If the session started on an earlier day, saves each past day's portion
     * under the correct dayKey and moves the start to today's midnight.
     */
    fun checkMidnightCrossing(now: Long) {
        var start = Store.runningStart.value ?: return
        val today = DateUtils.localDate(now)
        if (DateUtils.localDate(start) == today) return

        while (DateUtils.localDate(start).isBefore(today)) {
            val day = DateUtils.localDate(start)
            val nextMidnight = DateUtils.startOfDay(day.plusDays(1))
            val seconds = ((nextMidnight - start) / 1000).toInt()
            Store.addSession(nextMidnight - 1000, seconds, DateUtils.dayKey(day))
            start = nextMidnight
        }
        Store.setRunningStart(start)
    }

    /** Seconds of the active session that fall within the current day. */
    fun currentSessionSeconds(runningStart: Long?, now: Long): Int {
        if (runningStart == null) return 0
        val effectiveStart = maxOf(runningStart, DateUtils.startOfDay(now))
        return maxOf(0, ((now - effectiveStart) / 1000).toInt())
    }
}
