package com.allineami.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.allineami.app.data.DateUtils
import com.allineami.app.data.Store
import com.allineami.app.data.TimeFormat
import com.allineami.app.data.Tracker
import com.allineami.app.data.dailyBudgetSeconds
import kotlinx.coroutines.delay

@Composable
fun TodayScreen() {
    val context = LocalContext.current
    val sessions by Store.sessions.collectAsState()
    val runningStart by Store.runningStart.collectAsState()
    val targetHours by Store.targetWornHours.collectAsState()

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            Tracker.checkMidnightCrossing(now)
            delay(1000)
        }
    }

    // MARK: Calculations
    val isRunning = runningStart != null
    val budget = dailyBudgetSeconds(targetHours)
    val todayKey = DateUtils.dayKey(now)
    val usedSaved = sessions.filter { it.dayKey == todayKey }.sumOf { it.durationSeconds }
    val currentSession = Tracker.currentSessionSeconds(runningStart, now)
    val totalRemoved = usedSaved + currentSession
    val remaining = budget - totalRemoved
    val elapsedToday = maxOf(0, ((now - DateUtils.startOfDay(now)) / 1000).toInt())
    val wornToday = maxOf(0, elapsedToday - totalRemoved)
    val progress = (totalRemoved.toFloat() / maxOf(1, budget)).coerceIn(0f, 1f)

    val ringColor = when {
        remaining < 0 -> Red
        progress < 0.7f -> Green
        progress < 0.9f -> Orange
        else -> Red
    }
    val animatedProgress by animateFloatAsState(progress, tween(400), label = "progress")
    val animatedColor by animateColorAsState(ringColor, label = "ringColor")
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        Text(
            "Oggi",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        )

        // MARK: Ring gauge
        Box(Modifier.size(260.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 22.dp.toPx()
                val inset = stroke / 2
                val arcSize = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
                val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
                drawArc(trackColor, -90f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
                drawArc(
                    animatedColor, -90f, 360f * animatedProgress, false, topLeft, arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (isRunning) "Allineatore Tolto" else "Allineatore Montato",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    TimeFormat.hhmmss(remaining),
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (remaining >= 0) MaterialTheme.colorScheme.onSurface else Red,
                )
                Text(
                    if (remaining >= 0) "rimanenti" else "OVER",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (remaining >= 0) MaterialTheme.colorScheme.onSurface else Red,
                )
            }
        }

        // MARK: Toggle button
        Button(
            onClick = { if (isRunning) Tracker.stop(context) else Tracker.start(context) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRunning) Green else MaterialTheme.colorScheme.primary,
                contentColor = if (isRunning) Color.White else MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text(
                if (isRunning) "Monta Allineatore" else "Togli Allineatore",
                style = MaterialTheme.typography.titleMedium,
            )
        }

        // MARK: Stats grid
        val cards = buildList {
            add(Triple("Portate Oggi", TimeFormat.hhmmss(wornToday), Blue))
            add(Triple("Tolte Oggi", TimeFormat.hhmmss(totalRemoved), Orange))
            if (isRunning) add(Triple("Sessione Attiva", TimeFormat.hhmmss(currentSession), Red))
            add(Triple("Budget Giornaliero", TimeFormat.hhmmss(budget), Purple))
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            cards.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { (title, value, color) ->
                        StatCard(title, value, color, Modifier.weight(1f))
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}
