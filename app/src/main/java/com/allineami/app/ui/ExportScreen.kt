package com.allineami.app.ui

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.core.content.FileProvider
import java.io.File
import com.allineami.app.data.CsvBuilder
import com.allineami.app.data.DateUtils
import com.allineami.app.data.Store
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateLabelFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ITALIAN)

@Composable
fun ExportScreen() {
    val context = LocalContext.current
    val sessions by Store.sessions.collectAsState()

    var fromDate by rememberSaveable { mutableStateOf(DateUtils.today().minusDays(7)) }
    var toDate by rememberSaveable { mutableStateOf(DateUtils.today()) }
    // Only the preview goes in saved state: the full CSV may be too large for a Bundle
    var preview by rememberSaveable { mutableStateOf("") }
    var picking by remember { mutableStateOf<String?>(null) } // "from" | "to"

    fun generateAndShare() {
        // Swap the dates if reversed
        val (from, to) = if (fromDate <= toDate) fromDate to toDate else toDate to fromDate
        // include the whole "to" day up to 23:59:59
        val start = DateUtils.startOfDay(from)
        val endExclusive = DateUtils.startOfDay(to.plusDays(1))
        val filtered = sessions.filter { it.timestamp in start until endExclusive }
        val csv = CsvBuilder.buildSessionsCsv(filtered)
        preview = csv.lineSequence().take(10).joinToString("\n")

        // Share a cache file instead of text (no size limits)
        val dir = File(context.cacheDir, "export").apply { mkdirs() }
        val file = File(dir, "allineami_${from}_$to.csv").apply { writeText(csv) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "Esporta CSV"))
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            "Export CSV",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp),
        )

        Text("Range", style = MaterialTheme.typography.titleSmall)
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
            Column {
                ListItem(
                    headlineContent = { Text("Da") },
                    colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                    trailingContent = { Text(fromDate.format(dateLabelFormatter)) },
                    modifier = Modifier.clickable { picking = "from" },
                )
                ListItem(
                    headlineContent = { Text("A") },
                    colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                    trailingContent = { Text(toDate.format(dateLabelFormatter)) },
                    modifier = Modifier.clickable { picking = "to" },
                )
            }
        }

        Button(
            onClick = { generateAndShare() },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
        ) { Text("Genera CSV") }

        Text("Anteprima (prime righe)", style = MaterialTheme.typography.titleSmall)
        if (preview.isEmpty()) {
            Text("Genera un CSV per vedere l’anteprima.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            SelectionContainer {
                Text(
                    preview,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }

    picking?.let { which ->
        DateDialog(
            initial = if (which == "from") fromDate else toDate,
            onDismiss = { picking = null },
            onPick = { date ->
                if (which == "from") fromDate = date else toDate = date
                picking = null
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateDialog(initial: LocalDate, onDismiss: () -> Unit, onPick: (LocalDate) -> Unit) {
    // The Material3 DatePicker works in UTC millis
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let {
                    onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                } ?: onDismiss()
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } },
    ) {
        DatePicker(state = state)
    }
}
