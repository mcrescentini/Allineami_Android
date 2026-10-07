package com.allineami.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.allineami.app.data.DateUtils
import com.allineami.app.data.Store
import com.allineami.app.data.TimeFormat
import com.allineami.app.data.dailyBudgetSeconds
import com.allineami.app.data.targetWornSeconds
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor

private val italian = Locale.ITALIAN
private val dayLabelFormatter = DateTimeFormatter.ofPattern("EEE d", italian)
private val longDayLabelFormatter = DateTimeFormatter.ofPattern("EEE d MMM", italian)
private val weekTitleFormatter = DateTimeFormatter.ofPattern("d MMM", italian)

@Composable
fun StatsScreen() {
    val sessions by Store.sessions.collectAsState()
    val targetHours by Store.targetWornHours.collectAsState()

    var weekOffset by rememberSaveable { mutableIntStateOf(0) }
    var editingDate by remember { mutableStateOf<LocalDate?>(null) }
    var deletingDate by remember { mutableStateOf<LocalDate?>(null) }

    val weekStart = DateUtils.startOfWeek(DateUtils.today()).plusWeeks(weekOffset.toLong())
    val weekDays = (0L until 7L).map { weekStart.plusDays(it) }

    val removedByDay = sessions.groupBy { it.dayKey }.mapValues { (_, list) -> list.sumOf { it.durationSeconds } }
    val budget = dailyBudgetSeconds(targetHours)
    val targetWorn = targetWornSeconds(targetHours)

    fun removed(date: LocalDate) = removedByDay[DateUtils.dayKey(date)] ?: 0

    // Hours worn: real day length (23/25 h on DST changes); for today, only the time elapsed so far
    val today = DateUtils.today()
    fun worn(date: LocalDate): Int {
        val dayLength = if (date == today) {
            ((System.currentTimeMillis() - DateUtils.startOfDay(date)) / 1000).toInt()
        } else {
            DateUtils.daySeconds(date)
        }
        return maxOf(0, dayLength - removed(date))
    }
    fun hasData(date: LocalDate) = removedByDay.containsKey(DateUtils.dayKey(date))
    val daysWithData = weekDays.filter { hasData(it) }

    Column(Modifier.fillMaxSize()) {
        Text(
            "Statistiche",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 20.dp, top = 16.dp),
        )

        // MARK: Week navigator
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledTonalIconButton(onClick = { weekOffset -= 1 }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Settimana precedente")
            }
            Text(
                "${weekStart.format(weekTitleFormatter)} – ${weekStart.plusDays(6).format(weekTitleFormatter)}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            FilledTonalIconButton(onClick = { weekOffset += 1 }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Settimana successiva")
            }
        }

        // MARK: Chart
        val wornHours: List<Double?> = weekDays.map { date ->
            if (hasData(date)) worn(date) / 3600.0 else null
        }
        WeekChart(
            wornHours = wornHours,
            targetHours = targetHours,
            modifier = Modifier.fillMaxWidth().height(200.dp).padding(horizontal = 20.dp),
        )
        Spacer(Modifier.height(12.dp))

        // MARK: List
        if (daysWithData.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Nessun dato", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Nessuna sessione registrata questa settimana.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(daysWithData.reversed(), key = { it.toEpochDay() }) { date ->
                    val removedSec = removed(date)
                    val worn = worn(date)
                    val remaining = budget - removedSec
                    val onTarget = worn >= targetWorn

                    Row(
                        Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(10.dp).background(if (onTarget) Green else Red, CircleShape))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(date.format(dayLabelFormatter), style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Portate: ${TimeFormat.hhmmss(worn)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (onTarget) Green else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "Tolte: ${TimeFormat.hhmmss(removedSec)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (remaining >= 0) {
                                Text(
                                    "Rim: ${TimeFormat.hhmmss(remaining)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            } else {
                                Text(
                                    "Over: ${TimeFormat.hhmmss(abs(remaining))}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Red,
                                )
                            }
                        }
                        IconButton(onClick = { editingDate = date }) {
                            Icon(Icons.Default.Edit, contentDescription = "Modifica")
                        }
                        IconButton(onClick = { deletingDate = date }) {
                            Icon(Icons.Default.Delete, contentDescription = "Elimina", tint = Red)
                        }
                    }
                    HorizontalDivider(Modifier.padding(start = 42.dp))
                }
            }
        }
    }

    editingDate?.let { date ->
        EditDayDialog(
            date = date,
            initialSeconds = removed(date),
            onDismiss = { editingDate = null },
            onSave = { total ->
                val noon = DateUtils.startOfDay(date) + 12 * 3600 * 1000L
                Store.replaceDay(DateUtils.dayKey(date), total, noon)
                editingDate = null
            },
        )
    }

    deletingDate?.let { date ->
        AlertDialog(
            onDismissRequest = { deletingDate = null },
            title = { Text("Eliminare ${date.format(longDayLabelFormatter)}?") },
            text = { Text("Tutte le sessioni di questo giorno verranno cancellate.") },
            confirmButton = {
                TextButton(onClick = {
                    Store.deleteDay(DateUtils.dayKey(date))
                    deletingDate = null
                }) { Text("Elimina", color = Red) }
            },
            dismissButton = {
                TextButton(onClick = { deletingDate = null }) { Text("Annulla") }
            },
        )
    }
}

@Composable
private fun WeekChart(wornHours: List<Double?>, targetHours: Int, modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelStyle = TextStyle(fontSize = 10.sp, color = labelColor)

    val values = wornHours.filterNotNull()
    val minY = floor(minOf(targetHours.toDouble(), values.minOrNull() ?: targetHours.toDouble()) - 2)
        .coerceAtLeast(0.0)
    val maxY = 24.0

    Canvas(modifier) {
        val leftPad = 28.dp.toPx()
        val rightPad = 28.dp.toPx()
        val chartW = size.width - leftPad - rightPad
        val chartH = size.height
        fun x(i: Int) = leftPad + chartW * i / 6f
        fun y(v: Double) = (chartH * (1 - (v - minY) / (maxY - minY))).toFloat()

        // Grid and Y-axis labels
        val step = if (maxY - minY > 12) 4 else 2
        var g = (maxY - ((maxY - minY).toInt() / step) * step)
        while (g <= maxY) {
            drawLine(gridColor, Offset(leftPad, y(g)), Offset(leftPad + chartW, y(g)), 1f)
            val label = textMeasurer.measure("${g.toInt()}h", labelStyle)
            drawText(label, topLeft = Offset(0f, (y(g) - label.size.height / 2f).coerceIn(0f, chartH - label.size.height)))
            g += step
        }

        // Target line
        drawLine(
            Green.copy(alpha = 0.5f), Offset(leftPad, y(targetHours.toDouble())),
            Offset(leftPad + chartW, y(targetHours.toDouble())), 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 9f)),
        )
        val targetLabel = textMeasurer.measure("${targetHours}h", labelStyle.copy(color = Green))
        drawText(targetLabel, topLeft = Offset(leftPad + chartW + 4.dp.toPx(), y(targetHours.toDouble()) - targetLabel.size.height / 2f))

        // Line of hours worn (only days with data)
        val points = wornHours.mapIndexedNotNull { i, v -> v?.let { Offset(x(i), y(it)) to it } }
        if (points.size > 1) {
            val path = Path().apply {
                moveTo(points[0].first.x, points[0].first.y)
                points.drop(1).forEach { lineTo(it.first.x, it.first.y) }
            }
            drawPath(path, Blue, style = Stroke(2.5.dp.toPx()))
        }
        points.forEach { (p, v) ->
            drawCircle(if (v >= targetHours) Green else Red, radius = 4.dp.toPx(), center = p)
        }
    }
}

@Composable
private fun EditDayDialog(
    date: LocalDate,
    initialSeconds: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit,
) {
    var hours by remember { mutableStateOf((initialSeconds / 3600).toString()) }
    var minutes by remember { mutableStateOf(((initialSeconds % 3600) / 60).toString()) }
    var seconds by remember { mutableStateOf((initialSeconds % 60).toString()) }

    val h = hours.toIntOrNull()?.takeIf { it in 0..24 }
    val m = minutes.toIntOrNull()?.takeIf { it in 0..59 }
    val s = seconds.toIntOrNull()?.takeIf { it in 0..59 }
    val total = if (h != null && m != null && s != null) h * 3600 + m * 60 + s else null
    val valid = total != null && total <= DateUtils.daySeconds(date)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ore tolte per ${date.format(longDayLabelFormatter)}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField("Ore", hours, h != null, Modifier.weight(1f)) { hours = it }
                    NumberField("Min", minutes, m != null, Modifier.weight(1f)) { minutes = it }
                    NumberField("Sec", seconds, s != null, Modifier.weight(1f)) { seconds = it }
                }
                Text(
                    if (valid) "Totale tolte: ${TimeFormat.hhmmss(total!!)}" else "Valori non validi",
                    color = if (valid) MaterialTheme.colorScheme.onSurfaceVariant else Red,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(total!!) }, enabled = valid) { Text("Salva") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla") }
        },
    )
}

@Composable
private fun NumberField(
    label: String,
    value: String,
    isValid: Boolean,
    modifier: Modifier = Modifier,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { new -> if (new.length <= 2 && new.all { it.isDigit() }) onChange(new) },
        label = { Text(label) },
        isError = !isValid,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
    )
}
