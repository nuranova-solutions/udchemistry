package com.udchemistry.mobile.ui

import com.udchemistry.mobile.model.AttendanceRecord
import com.udchemistry.mobile.model.ClassRecord
import com.udchemistry.mobile.model.DashboardData
import com.udchemistry.mobile.model.DashboardMetric
import com.udchemistry.mobile.model.Institute
import com.udchemistry.mobile.model.PaymentRecord
import com.udchemistry.mobile.model.Profile
import com.udchemistry.mobile.model.QrCodeRecord
import com.udchemistry.mobile.model.Role
import com.udchemistry.mobile.model.Student
import com.udchemistry.mobile.model.TrendPoint
import java.time.LocalDate

internal fun previewMainUiState(role: Role = Role.Staff): MainUiState {
    val today = LocalDate.of(2026, 3, 30)
    val institute = Institute(
        id = "inst-1",
        name = "UD Chemistry",
        code = "UDC",
        address = "Colombo",
        contactNo = "0771234567",
        status = "active",
        createdAt = "2026-01-15",
    )
    val staffProfile = Profile(
        id = "staff-1",
        fullName = if (role == Role.Admin) "Admin User" else "Staff User",
        email = if (role == Role.Admin) "admin@udchemistry.app" else "staff@udchemistry.app",
        username = if (role == Role.Admin) "admin" else "staff",
        role = role,
        instituteId = if (role == Role.Admin) null else institute.id,
        phone = "0771234567",
        status = "active",
    )
    val staffMembers = listOf(
        staffProfile,
        Profile(
            id = "staff-2",
            fullName = "Nadeesha Perera",
            email = "nadeesha@udchemistry.app",
            username = "nadeesha",
            role = Role.Staff,
            instituteId = institute.id,
            phone = "0779876543",
            status = "active",
        ),
    )
    val students = listOf(
        Student(
            id = "stu-1",
            studentCode = "UD-001",
            fullName = "Akeel Fernando",
            alYear = 2027,
            instituteId = institute.id,
            monthlyFee = 1200.0,
            qrCodeId = "qr-1",
            whatsappNumber = "0771000001",
            qrLink = "https://example.com/qr/stu-1",
            joinedDate = "2026-01-05",
            status = "active",
        ),
        Student(
            id = "stu-2",
            studentCode = "UD-002",
            fullName = "Mihiri Silva",
            alYear = 2027,
            instituteId = institute.id,
            monthlyFee = 1200.0,
            qrCodeId = "qr-2",
            whatsappNumber = "0771000002",
            qrLink = "https://example.com/qr/stu-2",
            joinedDate = "2026-01-09",
            status = "active",
        ),
        Student(
            id = "stu-3",
            studentCode = "UD-003",
            fullName = "Dilan Raj",
            alYear = 2028,
            instituteId = institute.id,
            monthlyFee = 1500.0,
            qrCodeId = "qr-3",
            whatsappNumber = "0771000003",
            qrLink = "https://example.com/qr/stu-3",
            joinedDate = "2026-02-11",
            status = "active",
        ),
        Student(
            id = "stu-4",
            studentCode = "UD-004",
            fullName = "Kavini Jayasuriya",
            alYear = 2028,
            instituteId = institute.id,
            monthlyFee = 1500.0,
            qrCodeId = "qr-4",
            whatsappNumber = "0771000004",
            qrLink = "https://example.com/qr/stu-4",
            joinedDate = "2026-02-25",
            status = "active",
        ),
    )
    val classes = listOf(
        ClassRecord(
            id = "class-1",
            name = "2027 Theory",
            instituteId = institute.id,
            alYear = 2027,
            monthlyFee = 1200.0,
            classType = "general",
            weekday = "monday",
            startTime = "09:00:00",
            endTime = "11:00:00",
            weekOfMonth = null,
            activeFrom = "2026-01-01",
            activeUntil = null,
            status = "active",
            notes = "Main weekly theory class",
            createdAt = "2026-01-01T08:00:00",
        ),
        ClassRecord(
            id = "class-2",
            name = "2028 Revision",
            instituteId = institute.id,
            alYear = 2028,
            monthlyFee = 1500.0,
            classType = "general",
            weekday = "wednesday",
            startTime = "13:00:00",
            endTime = "15:00:00",
            weekOfMonth = null,
            activeFrom = "2026-01-08",
            activeUntil = null,
            status = "active",
            notes = "Revision batch",
            createdAt = "2026-01-08T08:00:00",
        ),
        ClassRecord(
            id = "class-3",
            name = "February Extra Lab",
            instituteId = institute.id,
            alYear = 2027,
            monthlyFee = 0.0,
            classType = "extra",
            weekday = "tuesday",
            startTime = "16:00:00",
            endTime = "18:00:00",
            weekOfMonth = 1,
            activeFrom = "2026-02-01",
            activeUntil = "2026-04-30",
            status = "active",
            notes = "First-week extra class for three months",
            createdAt = "2026-02-01T08:00:00",
        ),
    )
    val qrCodes = students.mapIndexed { index, student ->
        QrCodeRecord(
            id = "qr-${index + 1}",
            studentId = student.id,
            qrData = student.id,
            shareToken = "token-${index + 1}",
            qrLink = student.qrLink.orEmpty(),
            qrImagePath = "/tmp/${student.id}.png",
            qrImageUrl = null,
            lastSharedAt = today.minusDays(index.toLong()).toString(),
            generatedAt = today.minusDays(10).toString(),
        )
    }
    val attendance = listOf(
        AttendanceRecord(
            id = "att-1",
            studentId = "stu-1",
            attendanceDate = today.toString(),
            status = "present",
            markedBy = "staff-1",
            markedAt = "${today}T08:30:00",
        ),
        AttendanceRecord(
            id = "att-2",
            studentId = "stu-2",
            attendanceDate = today.toString(),
            status = "present",
            markedBy = "staff-1",
            markedAt = "${today}T08:32:00",
        ),
        AttendanceRecord(
            id = "att-3",
            studentId = "stu-3",
            attendanceDate = today.toString(),
            status = "late",
            markedBy = "staff-1",
            markedAt = "${today}T08:39:00",
        ),
    )
    val payments = listOf(
        PaymentRecord(
            id = "pay-1",
            studentId = "stu-1",
            paymentMonth = today.monthValue,
            paymentYear = today.year,
            amount = 1200.0,
            paid = true,
            paidDate = today.minusDays(3).toString(),
            markedBy = "staff-1",
        ),
        PaymentRecord(
            id = "pay-2",
            studentId = "stu-2",
            paymentMonth = today.monthValue,
            paymentYear = today.year,
            amount = 1200.0,
            paid = true,
            paidDate = today.minusDays(2).toString(),
            markedBy = "staff-1",
        ),
        PaymentRecord(
            id = "pay-3",
            studentId = "stu-3",
            paymentMonth = today.monthValue,
            paymentYear = today.year,
            amount = 1200.0,
            paid = false,
            paidDate = null,
            markedBy = "staff-1",
        ),
    )
    val dashboard = DashboardData(
        metrics = listOf(
            DashboardMetric("Total students", students.size, "Active roster in the workspace"),
            DashboardMetric("Attendance today", attendance.size, "Marked for the current day"),
            DashboardMetric("Paid this month", payments.count { it.paid }, "Monthly fees already settled"),
        ),
        attendanceTrend = listOf(
            TrendPoint("Mon", 2.0),
            TrendPoint("Tue", 3.0),
            TrendPoint("Wed", 4.0),
            TrendPoint("Thu", 3.0),
            TrendPoint("Fri", 4.0),
        ),
        incomeTrend = listOf(
            TrendPoint("Jan", 3200.0),
            TrendPoint("Feb", 4100.0),
            TrendPoint("Mar", 2400.0),
            TrendPoint("Apr", 4800.0),
        ),
        registrationTrend = listOf(
            TrendPoint("Jan", 1.0),
            TrendPoint("Feb", 2.0),
            TrendPoint("Mar", 1.0),
            TrendPoint("Apr", 3.0),
        ),
    )

    return MainUiState(
        bootstrapping = false,
        authenticated = true,
        profile = staffProfile,
        institutes = listOf(institute),
        classes = classes,
        staff = staffMembers,
        students = students,
        qrCodes = qrCodes,
        attendance = attendance,
        payments = payments,
        dashboard = dashboard,
        busy = false,
        refreshing = false,
    )
}
