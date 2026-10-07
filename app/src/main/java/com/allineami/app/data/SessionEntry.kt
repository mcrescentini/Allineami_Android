package com.allineami.app.data

/** A session during which the aligners were taken out. */
data class SessionEntry(
    val id: String,
    val timestamp: Long,
    val durationSeconds: Int,
    val dayKey: String,
    val note: String? = null,
)
