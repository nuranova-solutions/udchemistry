package com.udchemistry.mobile.util

import com.udchemistry.mobile.model.AttendanceRecord
import com.udchemistry.mobile.model.PaymentRecord
import com.udchemistry.mobile.model.Student
import com.udchemistry.mobile.model.TrendPoint
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

data class StudentMonthOption(
    val key: String,
    val label: String,
)

private val monthKeyFormatter = DateTimeFormatter.ofPattern("yyyy-MM")
private val monthLabelFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
private val monthChartFormatter = DateTimeFormatter.ofPattern("MMM yy")

private fun recentMonths(count: Int = 6): List<YearMonth> {
    val current = YearMonth.now()
    return (count - 1 downTo 0).map { offset -> current.minusMonths(offset.toLong()) }
}

private fun attendanceMonthKey(record: AttendanceRecord): String = record.attendanceDate.take(7)

private fun paymentMonthKey(record: PaymentRecord): String {
    return "%04d-%02d".format(record.paymentYear, record.paymentMonth)
}

fun currentMonthKey(): String = YearMonth.now().format(monthKeyFormatter)

fun buildStudentMonthOptions(
    student: Student,
    attendance: List<AttendanceRecord>,
    payments: List<PaymentRecord>,
): List<StudentMonthOption> {
    val monthKeys = linkedSetOf<String>()
    recentMonths().forEach { monthKeys += it.format(monthKeyFormatter) }
    monthKeys += currentMonthKey()
    monthKeys += student.joinedDate.take(7)
    attendance.forEach { monthKeys += attendanceMonthKey(it) }
    payments.forEach { monthKeys += paymentMonthKey(it) }
    return monthKeys
        .sortedDescending()
        .map { StudentMonthOption(it, formatMonthKeyLabel(it)) }
}

fun buildStudentAttendanceTrend(attendance: List<AttendanceRecord>): List<TrendPoint> {
    return recentMonths().map { month ->
        val monthKey = month.format(monthKeyFormatter)
        TrendPoint(
            label = month.format(monthChartFormatter),
            value = attendance.count {
                attendanceMonthKey(it) == monthKey && it.status.lowercase() != "absent"
            }.toDouble(),
        )
    }
}

fun buildStudentPaymentTrend(payments: List<PaymentRecord>): List<TrendPoint> {
    return recentMonths().map { month ->
        val monthKey = month.format(monthKeyFormatter)
        TrendPoint(
            label = month.format(monthChartFormatter),
            value = if (payments.any { paymentMonthKey(it) == monthKey && it.paid }) 1.0 else 0.0,
        )
    }
}

fun attendanceForMonth(attendance: List<AttendanceRecord>, monthKey: String): List<AttendanceRecord> {
    return attendance
        .filter { attendanceMonthKey(it) == monthKey }
        .sortedByDescending { it.attendanceDate }
}

fun paymentsForMonth(payments: List<PaymentRecord>, monthKey: String): List<PaymentRecord> {
    return payments
        .filter { paymentMonthKey(it) == monthKey }
        .sortedWith(compareByDescending<PaymentRecord> { it.paymentYear }.thenByDescending { it.paymentMonth })
}

fun countAttendedClasses(attendance: List<AttendanceRecord>): Int {
    return attendance.count { it.status.lowercase() != "absent" }
}

fun formatMonthKeyLabel(monthKey: String): String {
    return runCatching {
        YearMonth.parse(monthKey, monthKeyFormatter).format(monthLabelFormatter)
    }.getOrDefault(monthKey)
}

fun formatShortAttendanceDate(value: String): String {
    return runCatching {
        LocalDate.parse(value).format(DateTimeFormatter.ofPattern("dd MMM"))
    }.getOrDefault(value)
}
