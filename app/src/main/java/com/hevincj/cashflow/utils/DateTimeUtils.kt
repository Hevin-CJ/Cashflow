package com.hevincj.cashflow.utils

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateTimeUtils {
    fun formatTimestamp(timestampMs: Long): String {
        val zoneId = ZoneId.systemDefault()
        val date = Instant.ofEpochMilli(timestampMs).atZone(zoneId).toLocalDate()
        val now = LocalDate.now(zoneId)
        val yesterday = now.minusDays(1)
        
        return when (date) {
            now -> {
                val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault())
                Instant.ofEpochMilli(timestampMs).atZone(zoneId).toLocalTime().format(timeFormatter)
            }
            yesterday -> "Yesterday"
            else -> {
                val dateFormatter = DateTimeFormatter.ofPattern("MMM dd", Locale.getDefault())
                date.format(dateFormatter)
            }
        }
    }
}
