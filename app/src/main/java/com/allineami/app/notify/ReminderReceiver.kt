package com.allineami.app.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.allineami.app.data.Store

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Store.init(context)
        if (Store.runningStart.value == null) return
        val minutes = intent.getIntExtra(Reminders.EXTRA_MINUTES, 0)
        if (minutes > 0) Notifications.showReminder(context, minutes)
    }
}
