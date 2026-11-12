package com.example.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DateUtil {
    private val dateFormats = listOf(
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        },
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        },
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()),
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    )

    fun parseDate(dateStr: String?): Date? {
        if (dateStr.isNullOrBlank()) return null
        for (format in dateFormats) {
            try {
                val parsed = format.parse(dateStr.trim())
                if (parsed != null) return parsed
            } catch (_: Exception) { }
        }
        return null
    }

    fun calculateTimeLeft(expiresAt: String?): String {
        if (expiresAt.isNullOrBlank()) return "N/A"
        val date = parseDate(expiresAt) ?: return "Неизвестно"
        val diff = date.time - System.currentTimeMillis()
        if (diff <= 0) return "Истекло"

        val days = diff / (1000 * 60 * 60 * 24)
        val hours = (diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60)
        val mins = (diff % (1000 * 60 * 60)) / (1000 * 60)

        return if (days > 0) {
            "${days}д ${hours}ч"
        } else if (hours > 0) {
            "${hours}ч ${mins}м"
        } else {
            "${mins}м"
        }
    }

    fun isExpired(expiresAt: String?): Boolean {
        if (expiresAt.isNullOrBlank()) return false
        val date = parseDate(expiresAt) ?: return false
        return date.time <= System.currentTimeMillis()
    }
}
