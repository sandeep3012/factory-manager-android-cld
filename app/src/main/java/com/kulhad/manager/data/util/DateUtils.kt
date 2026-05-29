package com.kulhad.manager.data.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {

    private val dayFmt: SimpleDateFormat
        get() = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val monthFmt: SimpleDateFormat
        get() = SimpleDateFormat("MMM yyyy", Locale.getDefault())
    private val dayShortFmt: SimpleDateFormat
        get() = SimpleDateFormat("dd MMM", Locale.getDefault())
    private val timeFmt: SimpleDateFormat
        get() = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    private val auditTimeFmt: SimpleDateFormat
        get() = SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault())

    /** Normalize an epoch-millis timestamp to start-of-day in the device default zone. */
    fun startOfDay(timestamp: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /** End-of-day epoch millis (23:59:59.999). */
    fun endOfDay(timestamp: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }

    fun todayStart(): Long = startOfDay(System.currentTimeMillis())
    fun todayEnd(): Long = endOfDay(System.currentTimeMillis())

    fun startOfMonth(timestamp: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        cal.set(Calendar.DAY_OF_MONTH, 1)
        return startOfDay(cal.timeInMillis)
    }

    fun endOfMonth(timestamp: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        return endOfDay(cal.timeInMillis)
    }

    fun addDays(timestamp: Long, days: Int): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        cal.add(Calendar.DAY_OF_MONTH, days)
        return cal.timeInMillis
    }

    fun addMonths(timestamp: Long, months: Int): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        cal.add(Calendar.MONTH, months)
        return cal.timeInMillis
    }

    /** Returns the 7 day-start timestamps ending today (oldest first). */
    fun last7DayStarts(): List<Long> {
        val today = todayStart()
        return (6 downTo 0).map { addDays(today, -it) }
    }

    fun formatDay(ts: Long): String = dayFmt.format(Date(ts))
    fun formatDayShort(ts: Long): String = dayShortFmt.format(Date(ts))
    fun formatMonth(ts: Long): String = monthFmt.format(Date(ts))
    fun formatTime(ts: Long): String = timeFmt.format(Date(ts))

    /**
     * Formats an audit timestamp for display in [AuditInfoCard].
     *
     * Produces e.g. "5 Jun 2025, 3:45 PM" — includes year so records from prior years
     * are unambiguous, and uses single-digit day/hour to keep the string compact.
     *
     * Callers MUST check for [ts] == 0L before calling; 0L indicates a migrated row
     * that predates audit tracking and should be shown as "—" in the UI, not as an epoch date.
     */
    fun formatAuditTimestamp(ts: Long): String = auditTimeFmt.format(Date(ts))

    fun greeting(now: Long = System.currentTimeMillis()): String {
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        return when (cal.get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    /** Return labels M T W T F S S aligned to last7DayStarts(). */
    fun last7DayLabels(): List<String> {
        val labels = arrayOf("S", "M", "T", "W", "T", "F", "S")
        return last7DayStarts().map {
            val cal = Calendar.getInstance().apply { timeInMillis = it }
            labels[cal.get(Calendar.DAY_OF_WEEK) - 1]
        }
    }
}
