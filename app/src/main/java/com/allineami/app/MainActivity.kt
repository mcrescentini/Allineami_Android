package com.allineami.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.allineami.app.data.Store
import com.allineami.app.data.Tracker
import com.allineami.app.notify.Notifications
import com.allineami.app.notify.Reminders
import com.allineami.app.ui.AllineamiApp
import com.allineami.app.ui.AllineamiTheme

class MainActivity : ComponentActivity() {

    private val requestNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Store.init(this)
        Notifications.createChannels(this)

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            AllineamiTheme {
                AllineamiApp()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Tracker.checkMidnightCrossing(System.currentTimeMillis())
        Reminders.refreshExactAllowed(this)
        Store.runningStart.value?.let { start ->
            // Alarms are dropped if the user revokes "Alarms & reminders" or force-stops the app:
            // rescheduling is harmless because it replaces the existing ones
            Reminders.schedule(this, start)
            // On Android 14+ the ongoing notification can be swiped away: restore it if missing
            Notifications.showOngoingIfMissing(this, start)
        }
    }
}
