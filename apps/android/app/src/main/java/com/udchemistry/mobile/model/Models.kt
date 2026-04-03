package com.udchemistry.mobile.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Role {
    @SerialName("admin")
    Admin,

    @SerialName("staff")
    Staff,
}

@Serializable
data class Profile(
    val id: String,
    @SerialName("full_name") val fullName: String,
    val email: String,
    val username: String,
    val role: Role,
    @SerialName("institute_id") val instituteId: String? = null,
    val phone: String? = null,
    val status: String,
)

@Serializable
data class Institute(
    val id: String,
    val name: String,
    val code: String,
    val address: String? = null,
    @SerialName("contact_no") val contactNo: String? = null,
    val status: String,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class ClassRecord(
    val id: String,
    val name: String,
    @SerialName("institute_id") val instituteId: String,
    @SerialName("al_year") val alYear: Int,
    @SerialName("monthly_fee") val monthlyFee: Double = 0.0,
    @SerialName("class_type") val classType: String,
    val weekday: String,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String,
    @SerialName("week_of_month") val weekOfMonth: Int? = null,
    @SerialName("active_from") val activeFrom: String,
    @SerialName("active_until") val activeUntil: String? = null,
    val status: String,
    val notes: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class Student(
    val id: String,
    @SerialName("student_code") val studentCode: String? = null,
    @SerialName("full_name") val fullName: String,
    @SerialName("al_year") val alYear: Int,
    @SerialName("institute_id") val instituteId: String,
    @SerialName("monthly_fee") val monthlyFee: Double = 0.0,
    @SerialName("qr_code_id") val qrCodeId: String? = null,
    @SerialName("whatsapp_number") val whatsappNumber: String,
    @SerialName("qr_link") val qrLink: String? = null,
    @SerialName("joined_date") val joinedDate: String,
    val status: String,
)

@Serializable
data class QrCodeRecord(
    val id: String,
    @SerialName("student_id") val studentId: String,
    @SerialName("qr_data") val qrData: String,
    @SerialName("share_token") val shareToken: String,
    @SerialName("qr_link") val qrLink: String,
    @SerialName("qr_image_path") val qrImagePath: String,
    @SerialName("qr_image_url") val qrImageUrl: String? = null,
    @SerialName("last_shared_at") val lastSharedAt: String? = null,
    @SerialName("generated_at") val generatedAt: String,
)

@Serializable
data class AttendanceRecord(
    val id: String,
    @SerialName("student_id") val studentId: String,
    @SerialName("attendance_date") val attendanceDate: String,
    val status: String,
    @SerialName("marked_by") val markedBy: String? = null,
    @SerialName("marked_at") val markedAt: String,
)

@Serializable
data class PaymentRecord(
    val id: String,
    @SerialName("student_id") val studentId: String,
    @SerialName("payment_month") val paymentMonth: Int,
    @SerialName("payment_year") val paymentYear: Int,
    val amount: Double,
    val paid: Boolean,
    @SerialName("paid_date") val paidDate: String? = null,
    @SerialName("marked_by") val markedBy: String? = null,
)

@Serializable
data class AuthUser(
    val id: String,
    val email: String? = null,
)

@Serializable
data class AuthSessionResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_in") val expiresIn: Long,
    @SerialName("expires_at") val expiresAt: Long? = null,
    val user: AuthUser,
)

@Serializable
data class StoredSession(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
    val loginStartedAt: Long,
    val expiresAt: Long,
)

@Serializable
data class InstituteDraft(
    val name: String,
    val code: String,
    val address: String,
    val contactNo: String,
    val status: String,
)

@Serializable
data class ClassDraft(
    val name: String,
    val instituteId: String,
    val alYear: Int,
    val monthlyFee: Double,
    val classType: String,
    val weekday: String,
    val startTime: String,
    val endTime: String,
    val weekOfMonth: Int?,
    val activeFrom: String,
    val activeUntil: String?,
    val status: String,
    val notes: String,
)

@Serializable
data class StaffDraft(
    val fullName: String,
    val username: String,
    val email: String,
    val password: String = "",
    val instituteId: String,
    val phone: String,
    val status: String,
)

@Serializable
data class StudentDraft(
    val studentCode: String,
    val fullName: String,
    val alYear: Int,
    val instituteId: String,
    val monthlyFee: Double,
    val whatsappNumber: String,
    val joinedDate: String,
    val status: String,
)

@Serializable
data class AttendanceDraft(
    val studentId: String,
    val attendanceDate: String,
    val status: String,
)

@Serializable
data class PaymentDraft(
    val studentId: String,
    val paymentMonth: Int,
    val paymentYear: Int,
    val amount: Double,
    val paid: Boolean,
    val paidDate: String?,
)

data class ScanAttendanceResult(
    val duplicate: Boolean,
    val student: Student,
)

data class DashboardMetric(
    val label: String,
    val value: Int,
    val hint: String,
)

data class TrendPoint(
    val label: String,
    val value: Double,
)

data class DashboardData(
    val metrics: List<DashboardMetric> = emptyList(),
    val attendanceTrend: List<TrendPoint> = emptyList(),
    val incomeTrend: List<TrendPoint> = emptyList(),
    val registrationTrend: List<TrendPoint> = emptyList(),
)

data class AppSnapshot(
    val institutes: List<Institute>,
    val classes: List<ClassRecord>,
    val staff: List<Profile>,
    val students: List<Student>,
    val qrCodes: List<QrCodeRecord>,
    val attendance: List<AttendanceRecord>,
    val payments: List<PaymentRecord>,
    val dashboard: DashboardData,
)
