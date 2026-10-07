package com.allineami.app.data

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

object DateUtils {
    // Device time zone, read every time (the user may travel)
    val zone: ZoneId get() = ZoneId.systemDefault()
    private val dayKeyFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun dayKey(millis: Long): String = localDate(millis).format(dayKeyFormatter)

    fun dayKey(date: LocalDate): String = date.format(dayKeyFormatter)

    fun localDate(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

    fun today(): LocalDate = LocalDate.now(zone)

    fun startOfDay(date: LocalDate): Long = date.atStartOfDay(zone).toInstant().toEpochMilli()

    fun startOfDay(millis: Long): Long = startOfDay(localDate(millis))

    /** Real length of the day in seconds (23 or 25 hours on DST change days). */
    fun daySeconds(date: LocalDate): Int = ((startOfDay(date.plusDays(1)) - startOfDay(date)) / 1000).toInt()

    // Weeks start on Monday
    fun startOfWeek(date: LocalDate): LocalDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
}
