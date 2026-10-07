package com.allineami.app.notify

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.allineami.app.data.Store

/** When the user grants exact alarms, reschedule the pending reminders. */
class ExactAlarmPermissionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED) return
        Store.init(context)
        val start = Store.runningStart.value
        if (start == null) {
            Reminders.refreshExactAllowed(context)
            return
        }
        Reminders.cancel(context)
        Reminders.schedule(context, start)
    }
}
