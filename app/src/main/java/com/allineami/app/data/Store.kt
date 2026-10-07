package com.allineami.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Local persistence: sessions, the running session and settings.
 * The equivalent of SwiftData + UserDefaults in the iOS version.
 */
object Store {
    private const val PREFS = "allineami_rl7c"
    private const val KEY_SESSIONS = "sessions"
    private const val KEY_RUNNING_START = "running_start"
    private const val KEY_TARGET_HOURS = "target_worn_hours"
    private const val KEY_CORRUPT_BACKUP = "sessions_corrupt_backup"

    private lateinit var prefs: SharedPreferences

    private val _sessions = MutableStateFlow<List<SessionEntry>>(emptyList())
    val sessions: StateFlow<List<SessionEntry>> = _sessions.asStateFlow()

    // Start of the active session (millis), null when the aligners are in
    private val _runningStart = MutableStateFlow<Long?>(null)
    val runningStart: StateFlow<Long?> = _runningStart.asStateFlow()

    // Target hours to wear per day (default 22)
    private val _targetWornHours = MutableStateFlow(22)
    val targetWornHours: StateFlow<Int> = _targetWornHours.asStateFlow()

    @Synchronized
    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_SESSIONS, null)
        val decoded = decode(raw)
        if (decoded == null) {
            // Unreadable data: keep it aside instead of overwriting it on the next save
            prefs.edit().putString(KEY_CORRUPT_BACKUP, raw).apply()
        }
        _sessions.value = decoded ?: emptyList()
        _runningStart.value = prefs.getLong(KEY_RUNNING_START, -1L).takeIf { it > 0 }
        _targetWornHours.value = prefs.getInt(KEY_TARGET_HOURS, 22).takeIf { it > 0 } ?: 22
    }

    @Synchronized
    fun addSession(timestamp: Long, durationSeconds: Int, dayKey: String) {
        if (durationSeconds <= 0) return
        val entry = SessionEntry(UUID.randomUUID().toString(), timestamp, durationSeconds, dayKey)
        saveSessions(_sessions.value + entry)
    }

    /** Replaces all of a day's sessions with a single session of the given total. */
    @Synchronized
    fun replaceDay(dayKey: String, totalSeconds: Int, timestamp: Long) {
        val others = _sessions.value.filter { it.dayKey != dayKey }
        val replaced = if (totalSeconds > 0) {
            others + SessionEntry(UUID.randomUUID().toString(), timestamp, totalSeconds, dayKey)
        } else {
            others
        }
        saveSessions(replaced)
    }

    @Synchronized
    fun deleteDay(dayKey: String) {
        saveSessions(_sessions.value.filter { it.dayKey != dayKey })
    }

    @Synchronized
    fun setRunningStart(start: Long?) {
        _runningStart.value = start
        prefs.edit().putLong(KEY_RUNNING_START, start ?: -1L).apply()
    }

    @Synchronized
    fun setTargetWornHours(hours: Int) {
        _targetWornHours.value = hours
        prefs.edit().putInt(KEY_TARGET_HOURS, hours).apply()
    }

    private fun saveSessions(list: List<SessionEntry>) {
        val sorted = list.sortedByDescending { it.timestamp }
        _sessions.value = sorted
        prefs.edit().putString(KEY_SESSIONS, encode(sorted)).apply()
    }

    private fun encode(list: List<SessionEntry>): String {
        val arr = JSONArray()
        for (e in list) {
            arr.put(
                JSONObject()
                    .put("id", e.id)
                    .put("timestamp", e.timestamp)
                    .put("durationSeconds", e.durationSeconds)
                    .put("dayKey", e.dayKey)
                    .apply { e.note?.let { put("note", it) } }
            )
        }
        return arr.toString()
    }

    /** null if the JSON exists but can't be read. */
    private fun decode(json: String?): List<SessionEntry>? {
        if (json.isNullOrEmpty()) return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                SessionEntry(
                    id = o.getString("id"),
                    timestamp = o.getLong("timestamp"),
                    durationSeconds = o.getInt("durationSeconds"),
                    dayKey = o.getString("dayKey"),
                    note = if (o.has("note")) o.getString("note") else null,
                )
            }
        } catch (_: Exception) {
            null
        }
    }
}

// Budget = 24 - target hours
fun dailyBudgetSeconds(targetWornHours: Int): Int = (24 - targetWornHours) * 3600
fun targetWornSeconds(targetWornHours: Int): Int = targetWornHours * 3600
