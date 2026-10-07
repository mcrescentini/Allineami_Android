package com.allineami.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.allineami.app.data.Store
import com.allineami.app.notify.Reminders
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private val hoursRange = 10..23

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val saved by Store.targetWornHours.collectAsState()
    val exactAllowed by Reminders.exactAllowed.collectAsState()

    // Local copy so changes aren't applied until saved
    var pendingHours by remember { mutableIntStateOf(saved) }
    var justSaved by remember { mutableStateOf(false) }
    LaunchedEffect(justSaved) {
        if (justSaved) {
            delay(2000)
            justSaved = false
        }
    }

    val budget = 24 - pendingHours

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            "Impostazioni",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp),
        )

        Text("Obiettivo giornaliero", style = MaterialTheme.typography.titleSmall)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Devo portarlo per $pendingHours ore", style = MaterialTheme.typography.titleMedium)
                Slider(
                    value = pendingHours.toFloat(),
                    onValueChange = { pendingHours = it.roundToInt() },
                    valueRange = hoursRange.first.toFloat()..hoursRange.last.toFloat(),
                    steps = hoursRange.last - hoursRange.first - 1,
                )
            }
        }
        Text(
            "Con $pendingHours ore target, il tuo budget giornaliero senza allineatore è " +
                "$budget ${if (budget == 1) "ora" else "ore"}.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text("Riepilogo", style = MaterialTheme.typography.titleSmall)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Column {
                ListItem(
                    headlineContent = { Text("Ore con allineatore") },
                    colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                    trailingContent = { Text("$pendingHours h") },
                )
                ListItem(
                    headlineContent = { Text("Budget senza allineatore") },
                    colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                    trailingContent = { Text("$budget h") },
                )
            }
        }

        Button(
            onClick = {
                Store.setTargetWornHours(pendingHours)
                justSaved = true
            },
            enabled = pendingHours != saved,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text("Salva impostazioni", fontWeight = FontWeight.Bold)
        }

        if (Build.VERSION.SDK_INT >= 31) {
            Text("Promemoria", style = MaterialTheme.typography.titleSmall)
            Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        if (exactAllowed) {
                            "I promemoria ogni 15 minuti arrivano puntuali."
                        } else {
                            "Senza il permesso \"Sveglie e promemoria\", Android può ritardare " +
                                "i promemoria anche di 10-20 minuti."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (!exactAllowed) {
                        OutlinedButton(
                            onClick = {
                                val packageUri = Uri.parse("package:${context.packageName}")
                                try {
                                    context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, packageUri))
                                } catch (_: android.content.ActivityNotFoundException) {
                                    // Some ROMs don't have the dedicated screen
                                    context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri))
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Attiva promemoria puntuali") }
                    }
                }
            }
        }

        if (justSaved) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Green)
                Spacer(Modifier.width(6.dp))
                Text("Salvato!", color = Green)
            }
        }
        Credits()
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Credits() {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull()
    }

    Column(
        Modifier.fillMaxWidth().padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            "Allineami${version?.let { " $it" } ?: ""}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "Realizzata da",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.TextButton(onClick = { uriHandler.openUri("https://rootlabs.it/") }) {
                Text("RootLabs")
            }
            Text("·", color = MaterialTheme.colorScheme.onSurfaceVariant)
            androidx.compose.material3.TextButton(onClick = { uriHandler.openUri("https://crescentinistudio.it/") }) {
                Text("crescentinistudio.it")
            }
        }
        Text(
            "Codice sorgente open source con licenza GPL-3.0",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
