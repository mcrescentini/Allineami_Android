package com.allineami.app.data

import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object CsvBuilder {
    // Sessions: one row per entry
    fun buildSessionsCsv(entries: List<SessionEntry>): String {
        val iso = DateTimeFormatter.ISO_OFFSET_DATE_TIME
        val lines = mutableListOf("timestamp_iso,day,duration_seconds,duration_mmss,note")
        for (e in entries.sortedBy { it.timestamp }) {
            val ts = Instant.ofEpochMilli(e.timestamp).atZone(DateUtils.zone)
                .truncatedTo(ChronoUnit.MILLIS).format(iso)
            val dur = TimeFormat.mmss(e.durationSeconds)
            lines += "$ts,${e.dayKey},${e.durationSeconds},$dur,${csvField(e.note.orEmpty())}"
        }
        return lines.joinToString("\n")
    }

    // Quote the field and prefix = + - @ with an apostrophe so Excel won't run it as a formula
    private fun csvField(raw: String): String {
        val guarded = if (raw.isNotEmpty() && raw[0] in "=+-@\t\r") "'$raw" else raw
        return "\"" + guarded.replace("\"", "\"\"") + "\""
    }
}
