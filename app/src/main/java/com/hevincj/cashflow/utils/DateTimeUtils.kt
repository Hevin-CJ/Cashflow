package com.hevincj.cashflow.utils

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateTimeUtils {
    private val zoneId: ZoneId = ZoneId.systemDefault()
    private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault())
    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM dd", Locale.getDefault())

    fun formatTimestamp(timestampMs: Long): String {
        val date = Instant.ofEpochMilli(timestampMs).atZone(zoneId).toLocalDate()
        val now = LocalDate.now(zoneId)
        val yesterday = now.minusDays(1)
        
        return when (date) {
            now -> Instant.ofEpochMilli(timestampMs).atZone(zoneId).toLocalTime().format(timeFormatter)
            yesterday -> "Yesterday"
            else -> date.format(dateFormatter)
        }
    }

    fun formatDueDate(timestampMs: Long): String {
        val date = Instant.ofEpochMilli(timestampMs).atZone(zoneId).toLocalDate()
        return date.format(dateFormatter)
    }
}
