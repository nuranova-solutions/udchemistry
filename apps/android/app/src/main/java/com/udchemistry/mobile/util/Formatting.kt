package com.udchemistry.mobile.util

import java.text.NumberFormat
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

fun formatCurrency(value: Double): String {
    val locale = Locale.Builder()
        .setLanguage("en")
        .setRegion("LK")
        .build()
    return NumberFormat.getCurrencyInstance(locale).apply {
        maximumFractionDigits = 2
    }.format(value)
}

fun formatDate(value: String?): String {
    if (value.isNullOrBlank()) return "-"
    return runCatching {
        if (value.contains("T")) {
            OffsetDateTime.parse(value).format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"))
        } else {
            LocalDate.parse(value).format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
        }
    }.getOrDefault(value)
}
