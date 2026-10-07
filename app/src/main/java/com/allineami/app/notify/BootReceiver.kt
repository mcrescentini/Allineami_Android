package com.allineami.app.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.allineami.app.data.Store

/** Alarms are cleared after a reboot or an app update: restore them. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return
        Store.init(context)
        val start = Store.runningStart.value ?: return
        Notifications.createChannels(context)
        Reminders.schedule(context, start)
        Notifications.showOngoing(context, start)
    }
}
