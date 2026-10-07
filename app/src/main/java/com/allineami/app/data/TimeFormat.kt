package com.allineami.app.data

import java.util.Locale
import kotlin.math.abs

object TimeFormat {
    fun mmss(totalSeconds: Int): String {
        val s = abs(totalSeconds)
        return String.format(Locale.ROOT, "%02d:%02d", s / 60, s % 60)
    }

    fun hhmmss(totalSeconds: Int): String {
        val s = abs(totalSeconds)
        return String.format(Locale.ROOT, "%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60)
    }
}
