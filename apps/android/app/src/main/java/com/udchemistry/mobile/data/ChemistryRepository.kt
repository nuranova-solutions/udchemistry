package com.udchemistry.mobile.data

import android.content.Context
import com.udchemistry.mobile.BuildConfig
import com.udchemistry.mobile.model.AppSnapshot
import com.udchemistry.mobile.model.AttendanceDraft
import com.udchemistry.mobile.model.AttendanceRecord
import com.udchemistry.mobile.model.AuthSessionResponse
import com.udchemistry.mobile.model.ClassDraft
import com.udchemistry.mobile.model.ClassRecord
import com.udchemistry.mobile.model.DashboardData
import com.udchemistry.mobile.model.DashboardMetric
import com.udchemistry.mobile.model.Institute
import com.udchemistry.mobile.model.InstituteDraft
import com.udchemistry.mobile.model.PaymentDraft
import com.udchemistry.mobile.model.PaymentRecord
import com.udchemistry.mobile.model.Profile
import com.udchemistry.mobile.model.QrCodeRecord
import com.udchemistry.mobile.model.Role
import com.udchemistry.mobile.model.ScanAttendanceResult
import com.udchemistry.mobile.model.StoredSession
import com.udchemistry.mobile.model.StaffDraft
import com.udchemistry.mobile.model.Student
import com.udchemistry.mobile.model.StudentDraft
import com.udchemistry.mobile.model.TrendPoint
import com.udchemistry.mobile.util.QrCodeUtils
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import java.io.Closeable
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.UUID
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject

class SessionExpiredException(message: String) : IllegalStateException(message)

class ChemistryRepository(private val context: Context) : Closeable {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(json)
        }
    }

    private val sessionStore = SessionStore(context)
    private val sessionMutex = Mutex()
    private val supabaseUrl = BuildConfig.SUPABASE_URL.removeSuffix("/")
    private val publishableKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY
    private val publicAppUrl = BuildConfig.APP_PUBLIC_URL.removeSuffix("/")
    private var classesSupported: Boolean? = null
    private var studentMonthlyFeeSupported: Boolean? = null

    suspend fun restoreSession(): Profile? {
        val storedSession = sessionStore.read() ?: return null
        if (isSessionPastLimit(storedSession)) {
            sessionStore.clear()
            return null
        }

        val refreshedSession = refreshSession(storedSession)
        val profile = fetchProfile(refreshedSession.userId)
        if (profile.status != "active") {
            signOut()
            return null
        }
        return profile
    }

    suspend fun signIn(username: String, password: String): Profile {
        val email = resolveEmail(username)
        val response = client.post("$supabaseUrl/auth/v1/token") {
            parameter("grant_type", "password")
            header("apikey", publishableKey)
            contentType(ContentType.Application.Json)
            setBody(encodeRequestBody(mapOf("email" to email, "password" to password)))
        }
        ensureSuccess(response)
        val session = response.body<AuthSessionResponse>()
        sessionStore.save(
            StoredSession(
                accessToken = session.accessToken,
                refreshToken = session.refreshToken,
                userId = session.user.id,
                loginStartedAt = System.currentTimeMillis(),
                expiresAt = resolveExpiry(session),
            ),
        )

        val profile = fetchProfile(session.user.id)
        if (profile.status != "active") {
            signOut()
            throw IllegalStateException("This account is inactive. Please contact the admin.")
        }
        return profile
    }

    suspend fun signOut() {
        sessionStore.clear()
    }

    suspend fun loadSnapshot(profile: Profile): AppSnapshot = coroutineScope {
        val institutesDeferred = async { fetchInstitutes(profile) }
        val classesDeferred = async { fetchClasses(profile) }
        val staffDeferred = async { fetchStaff(profile) }
        val studentsDeferred = async { fetchStudents(profile) }

        val students = studentsDeferred.await()
        val qrCodesDeferred = async { fetchQrCodes(students) }
        val attendanceDeferred = async { fetchAttendance(profile, students) }
        val paymentsDeferred = async { fetchPayments(profile, students) }

        val institutes = institutesDeferred.await()
        val classes = classesDeferred.await()
        val staff = staffDeferred.await()
        val qrCodes = qrCodesDeferred.await()
        val attendance = attendanceDeferred.await()
        val payments = paymentsDeferred.await()

        AppSnapshot(
            institutes = institutes,
            classes = classes,
            staff = staff,
            students = students,
            qrCodes = qrCodes,
            attendance = attendance,
            payments = payments,
            dashboard = buildDashboard(students, attendance, payments),
        )
    }

    suspend fun createInstitute(draft: InstituteDraft) {
        authorizedRestPost(
            "institutes",
            mapOf(
                "name" to draft.name.trim(),
                "code" to draft.code.trim(),
                "address" to draft.address.trim().ifBlank { null },
                "contact_no" to draft.contactNo.trim().ifBlank { null },
                "status" to draft.status,
            ),
        )
    }

    suspend fun createClass(profile: Profile, draft: ClassDraft) {
        ensureClassesSupported()
        authorizedRestPost(
            "classes",
            mapOf(
                "name" to draft.name.trim(),
                "institute_id" to draft.instituteId,
                "al_year" to draft.alYear,
                "monthly_fee" to draft.monthlyFee,
                "class_type" to draft.classType,
                "weekday" to draft.weekday,
                "start_time" to normalizeTimeValue(draft.startTime),
                "end_time" to normalizeTimeValue(draft.endTime),
                "week_of_month" to if (draft.classType == "extra") draft.weekOfMonth else null,
                "active_from" to draft.activeFrom,
                "active_until" to draft.activeUntil?.trim().orEmpty().ifBlank { null },
                "status" to draft.status,
                "notes" to draft.notes.trim().ifBlank { null },
                "created_by" to profile.id,
            ),
        )
    }

    suspend fun updateClass(profile: Profile, classId: String, draft: ClassDraft) {
        ensureClassesSupported()
        authorizedRestPatch(
            "classes",
            mapOf("id" to "eq.$classId"),
            mapOf(
                "name" to draft.name.trim(),
                "institute_id" to draft.instituteId,
                "al_year" to draft.alYear,
                "monthly_fee" to draft.monthlyFee,
                "class_type" to draft.classType,
                "weekday" to draft.weekday,
                "start_time" to normalizeTimeValue(draft.startTime),
                "end_time" to normalizeTimeValue(draft.endTime),
                "week_of_month" to if (draft.classType == "extra") draft.weekOfMonth else null,
                "active_from" to draft.activeFrom,
                "active_until" to draft.activeUntil?.trim().orEmpty().ifBlank { null },
                "status" to draft.status,
                "notes" to draft.notes.trim().ifBlank { null },
                "created_by" to profile.id,
            ),
        )
    }

    suspend fun deleteClass(id: String) {
        ensureClassesSupported()
        authorizedRestDelete("classes", mapOf("id" to "eq.$id"))
    }

    suspend fun updateInstitute(id: String, draft: InstituteDraft) {
        authorizedRestPatch(
            "institutes",
            mapOf("id" to "eq.$id"),
            mapOf(
                "name" to draft.name.trim(),
                "code" to draft.code.trim(),
                "address" to draft.address.trim().ifBlank { null },
                "contact_no" to draft.contactNo.trim().ifBlank { null },
                "status" to draft.status,
            ),
        )
    }

    suspend fun deleteInstitute(id: String) {
        authorizedRestDelete("institutes", mapOf("id" to "eq.$id"))
    }

    suspend fun createStaff(draft: StaffDraft) {
        authorizedRpc(
            "admin_create_staff",
            mapOf(
                "p_full_name" to draft.fullName.trim(),
                "p_username" to draft.username.trim(),
                "p_email" to draft.email.trim().lowercase(),
                "p_password" to draft.password,
                "p_institute_id" to draft.instituteId,
                "p_phone" to draft.phone.trim().ifBlank { null },
                "p_status" to draft.status,
            ),
        )
    }

    suspend fun updateStaff(id: String, draft: StaffDraft) {
        authorizedRpc(
            "admin_update_staff",
            mapOf(
                "p_staff_id" to id,
                "p_full_name" to draft.fullName.trim(),
                "p_username" to draft.username.trim(),
                "p_email" to draft.email.trim().lowercase(),
                "p_password" to draft.password.trim().ifBlank { null },
                "p_institute_id" to draft.instituteId,
                "p_phone" to draft.phone.trim().ifBlank { null },
                "p_status" to draft.status,
            ),
        )
    }

    suspend fun deleteStaff(id: String) {
        authorizedRpc("admin_delete_staff", mapOf("p_staff_id" to id))
    }

    suspend fun createStudent(profile: Profile, draft: StudentDraft) {
        val studentId = UUID.randomUUID().toString()
        val qrCodeId = UUID.randomUUID().toString()
        val shareToken = UUID.randomUUID().toString().replace("-", "")
        val qrLink = "$publicAppUrl/qr/$shareToken"
        val supportsMonthlyFee = supportsStudentMonthlyFee()

        try {
            authorizedRestPost(
                "students",
                buildMap<String, Any?> {
                    put("id", studentId)
                    put("student_code", draft.studentCode.trim().ifBlank { null })
                    put("full_name", draft.fullName.trim())
                    put("al_year", draft.alYear)
                    put("institute_id", draft.instituteId)
                    put("qr_code_id", qrCodeId)
                    put("whatsapp_number", draft.whatsappNumber.trim())
                    put("qr_link", qrLink)
                    put("joined_date", draft.joinedDate)
                    put("status", draft.status)
                    put("created_by", profile.id)
                    if (supportsMonthlyFee) {
                        put("monthly_fee", draft.monthlyFee)
                    }
                },
            )
            authorizedRestPost(
                "qr_codes",
                mapOf(
                    "id" to qrCodeId,
                    "student_id" to studentId,
                    "qr_data" to studentId,
                    "share_token" to shareToken,
                    "qr_link" to qrLink,
                    "qr_image_path" to "generated/$shareToken.png",
                    "qr_image_url" to QrCodeUtils.createPngDataUrl(studentId),
                ),
            )
        } catch (error: Exception) {
            runCatching { authorizedRestDelete("students", mapOf("id" to "eq.$studentId")) }
            throw error
        }
    }

    suspend fun updateStudent(student: Student, draft: StudentDraft) {
        val supportsMonthlyFee = supportsStudentMonthlyFee()
        authorizedRestPatch(
            "students",
            mapOf("id" to "eq.${student.id}"),
            buildMap<String, Any?> {
                put("student_code", draft.studentCode.trim().ifBlank { null })
                put("full_name", draft.fullName.trim())
                put("al_year", draft.alYear)
                put("institute_id", draft.instituteId)
                put("whatsapp_number", draft.whatsappNumber.trim())
                put("joined_date", draft.joinedDate)
                put("status", draft.status)
                if (supportsMonthlyFee) {
                    put("monthly_fee", draft.monthlyFee)
                }
            },
        )

        if (student.qrCodeId.isNullOrBlank() || student.qrLink.isNullOrBlank()) {
            val qrCodeId = student.qrCodeId ?: UUID.randomUUID().toString()
            val shareToken = UUID.randomUUID().toString().replace("-", "")
            val qrLink = "$publicAppUrl/qr/$shareToken"
            authorizedRestPatch(
                "students",
                mapOf("id" to "eq.${student.id}"),
                mapOf("qr_code_id" to qrCodeId, "qr_link" to qrLink),
            )
            authorizedRestPost(
                "qr_codes",
                mapOf(
                    "id" to qrCodeId,
                    "student_id" to student.id,
                    "qr_data" to student.id,
                    "share_token" to shareToken,
                    "qr_link" to qrLink,
                    "qr_image_path" to "generated/$shareToken.png",
                    "qr_image_url" to QrCodeUtils.createPngDataUrl(student.id),
                ),
            )
        }
    }

    suspend fun deleteStudent(id: String) {
        authorizedRestDelete("students", mapOf("id" to "eq.$id"))
    }

    suspend fun createAttendance(profile: Profile, draft: AttendanceDraft) {
        authorizedRestPost(
            "attendance",
            mapOf(
                "student_id" to draft.studentId,
                "attendance_date" to draft.attendanceDate,
                "status" to draft.status,
                "marked_by" to profile.id,
            ),
        )
    }

    suspend fun updateAttendance(profile: Profile, attendanceId: String, draft: AttendanceDraft) {
        authorizedRestPatch(
            "attendance",
            mapOf("id" to "eq.$attendanceId"),
            mapOf(
                "student_id" to draft.studentId,
                "attendance_date" to draft.attendanceDate,
                "status" to draft.status,
                "marked_by" to profile.id,
                "marked_at" to OffsetDateTime.now().toString(),
            ),
        )
    }

    suspend fun deleteAttendance(id: String) {
        authorizedRestDelete("attendance", mapOf("id" to "eq.$id"))
    }

    suspend fun createPayment(profile: Profile, draft: PaymentDraft) {
        authorizedRestPost(
            "payments",
            mapOf(
                "student_id" to draft.studentId,
                "payment_month" to draft.paymentMonth,
                "payment_year" to draft.paymentYear,
                "amount" to draft.amount,
                "paid" to draft.paid,
                "paid_date" to if (draft.paid) draft.paidDate ?: LocalDate.now().toString() else null,
                "marked_by" to profile.id,
            ),
        )
    }

    suspend fun updatePayment(profile: Profile, paymentId: String, draft: PaymentDraft) {
        authorizedRestPatch(
            "payments",
            mapOf("id" to "eq.$paymentId"),
            mapOf(
                "student_id" to draft.studentId,
                "payment_month" to draft.paymentMonth,
                "payment_year" to draft.paymentYear,
                "amount" to draft.amount,
                "paid" to draft.paid,
                "paid_date" to if (draft.paid) draft.paidDate ?: LocalDate.now().toString() else null,
                "marked_by" to profile.id,
            ),
        )
    }

    suspend fun deletePayment(id: String) {
        authorizedRestDelete("payments", mapOf("id" to "eq.$id"))
    }

    suspend fun scanAttendance(profile: Profile, qrData: String): ScanAttendanceResult {
        val supportsMonthlyFee = supportsStudentMonthlyFee()
        val studentFilters = buildMap {
            put("select", studentSelectFields(supportsMonthlyFee))
            put("id", "eq.${qrData.trim()}")
            if (profile.role == Role.Staff && !profile.instituteId.isNullOrBlank()) {
                put("institute_id", "eq.${profile.instituteId}")
            }
        }
        val student = authorizedRestGetList<Student>("students", studentFilters).firstOrNull()
            ?: throw IllegalStateException("Student QR was not found in your access scope.")

        val today = LocalDate.now().toString()
        val existing = authorizedRestGetList<AttendanceRecord>(
            "attendance",
            mapOf(
                "select" to "id,student_id,attendance_date,status,marked_by,marked_at",
                "student_id" to "eq.${student.id}",
                "attendance_date" to "eq.$today",
            ),
        )
        if (existing.isNotEmpty()) {
            return ScanAttendanceResult(duplicate = true, student = student)
        }

        createAttendance(profile, AttendanceDraft(student.id, today, "present"))
        return ScanAttendanceResult(duplicate = false, student = student)
    }

    suspend fun markCurrentMonthPaid(profile: Profile, studentId: String) {
        val now = LocalDate.now()
        val existing = authorizedRestGetList<PaymentRecord>(
            "payments",
            mapOf(
                "select" to "id,student_id,payment_month,payment_year,amount,paid,paid_date,marked_by",
                "student_id" to "eq.$studentId",
                "payment_month" to "eq.${now.monthValue}",
                "payment_year" to "eq.${now.year}",
            ),
        ).firstOrNull()

        if (existing == null) {
            val amount = fetchStudentById(profile, studentId)?.monthlyFee ?: 0.0
            createPayment(
                profile,
                PaymentDraft(studentId, now.monthValue, now.year, amount, true, now.toString()),
            )
        } else {
            updatePayment(
                profile,
                existing.id,
                PaymentDraft(studentId, now.monthValue, now.year, existing.amount, true, now.toString()),
            )
        }
    }

    suspend fun updateProfile(profileId: String, fullName: String, phone: String?) {
        authorizedRestPatch(
            "profiles",
            mapOf("id" to "eq.$profileId"),
            mapOf(
                "full_name" to fullName.trim(),
                "phone" to phone?.trim().orEmpty().ifBlank { null },
            ),
        )
    }

    suspend fun updatePassword(password: String) {
        val session = requireSession()
        val response = client.put("$supabaseUrl/auth/v1/user") {
            header("apikey", publishableKey)
            header(HttpHeaders.Authorization, "Bearer ${session.accessToken}")
            contentType(ContentType.Application.Json)
            setBody(encodeRequestBody(mapOf("password" to password)))
        }
        ensureSuccess(response)
    }

    suspend fun fetchQrCodeForStudent(studentId: String): QrCodeRecord? {
        return authorizedRestGetList<QrCodeRecord>(
            "qr_codes",
            mapOf(
                "select" to "id,student_id,qr_data,share_token,qr_link,qr_image_path,qr_image_url,last_shared_at,generated_at",
                "student_id" to "eq.$studentId",
            ),
        ).firstOrNull()
    }

    private suspend fun fetchInstitutes(profile: Profile): List<Institute> {
        val query = mutableMapOf(
            "select" to "id,name,code,address,contact_no,status,created_at",
            "order" to "created_at.desc",
        )
        if (profile.role == Role.Staff && !profile.instituteId.isNullOrBlank()) {
            query["id"] = "eq.${profile.instituteId}"
        }
        return authorizedRestGetList("institutes", query)
    }

    private suspend fun fetchClasses(profile: Profile): List<ClassRecord> {
        if (!supportsClasses()) return emptyList()
        val query = mutableMapOf(
            "select" to classSelectFields(),
            "order" to "created_at.desc",
        )
        if (profile.role == Role.Staff && !profile.instituteId.isNullOrBlank()) {
            query["institute_id"] = "eq.${profile.instituteId}"
        }
        return authorizedRestGetList("classes", query)
    }

    private suspend fun fetchStaff(profile: Profile): List<Profile> {
        val query = mutableMapOf(
            "select" to "id,full_name,email,username,role,institute_id,phone,status",
            "role" to "eq.staff",
            "order" to "full_name.asc",
        )
        if (profile.role == Role.Staff && !profile.instituteId.isNullOrBlank()) {
            query["institute_id"] = "eq.${profile.instituteId}"
        }
        return authorizedRestGetList("profiles", query)
    }

    private suspend fun fetchStudents(profile: Profile): List<Student> {
        val supportsMonthlyFee = supportsStudentMonthlyFee()
        val query = mutableMapOf(
            "select" to studentSelectFields(supportsMonthlyFee),
            "order" to "created_at.desc",
        )
        if (profile.role == Role.Staff && !profile.instituteId.isNullOrBlank()) {
            query["institute_id"] = "eq.${profile.instituteId}"
        }
        return authorizedRestGetList("students", query)
    }

    private suspend fun fetchStudentById(profile: Profile, studentId: String): Student? {
        val supportsMonthlyFee = supportsStudentMonthlyFee()
        val query = buildMap {
            put("select", studentSelectFields(supportsMonthlyFee))
            put("id", "eq.$studentId")
            if (profile.role == Role.Staff && !profile.instituteId.isNullOrBlank()) {
                put("institute_id", "eq.${profile.instituteId}")
            }
        }
        return authorizedRestGetList<Student>("students", query).firstOrNull()
    }

    private suspend fun fetchQrCodes(students: List<Student>): List<QrCodeRecord> {
        if (students.isEmpty()) return emptyList()
        val filter = students.joinToString(",") { it.id }
        return authorizedRestGetList(
            "qr_codes",
            mapOf(
                "select" to "id,student_id,qr_data,share_token,qr_link,qr_image_path,qr_image_url,last_shared_at,generated_at",
                "student_id" to "in.($filter)",
                "order" to "generated_at.desc",
            ),
        )
    }

    private suspend fun fetchAttendance(profile: Profile, students: List<Student>): List<AttendanceRecord> {
        val query = mutableMapOf(
            "select" to "id,student_id,attendance_date,status,marked_by,marked_at",
            "order" to "marked_at.desc",
        )
        if (profile.role == Role.Staff) {
            if (students.isEmpty()) return emptyList()
            query["student_id"] = "in.(${students.joinToString(",") { it.id }})"
        }
        return authorizedRestGetList("attendance", query)
    }

    private suspend fun fetchPayments(profile: Profile, students: List<Student>): List<PaymentRecord> {
        val query = mutableMapOf(
            "select" to "id,student_id,payment_month,payment_year,amount,paid,paid_date,marked_by",
            "order" to "payment_year.desc,payment_month.desc",
        )
        if (profile.role == Role.Staff) {
            if (students.isEmpty()) return emptyList()
            query["student_id"] = "in.(${students.joinToString(",") { it.id }})"
        }
        return authorizedRestGetList("payments", query)
    }

    private suspend fun fetchProfile(userId: String): Profile {
        return authorizedRestGetList<Profile>(
            "profiles",
            mapOf(
                "select" to "id,full_name,email,username,role,institute_id,phone,status",
                "id" to "eq.$userId",
            ),
        ).firstOrNull() ?: throw IllegalStateException("Your profile record is missing from Supabase.")
    }

    private suspend fun resolveEmail(username: String): String {
        val response = client.post("$supabaseUrl/rest/v1/rpc/get_login_email_by_username") {
            header("apikey", publishableKey)
            contentType(ContentType.Application.Json)
            setBody(encodeRequestBody(mapOf("p_username" to username.trim())))
        }
        ensureSuccess(response)
        return json.decodeFromString(response.bodyAsText())
    }

    private suspend fun supportsStudentMonthlyFee(): Boolean {
        studentMonthlyFeeSupported?.let { return it }

        val session = requireSession()
        val response = client.get("$supabaseUrl/rest/v1/students") {
            header("apikey", publishableKey)
            header(HttpHeaders.Authorization, "Bearer ${session.accessToken}")
            parameter("select", "monthly_fee")
            parameter("limit", "1")
        }
        val body = response.bodyAsText()

        if (response.status.isSuccess()) {
            studentMonthlyFeeSupported = true
            return true
        }

        if (body.contains("students.monthly_fee", ignoreCase = true) && body.contains("does not exist", ignoreCase = true)) {
            studentMonthlyFeeSupported = false
            return false
        }

        val message = try {
            json.decodeFromString<ApiError>(body).bestMessage()
        } catch (_: SerializationException) {
            body.ifBlank { "Request failed with ${response.status.value}." }
        }
        throw IllegalStateException(message)
    }

    private suspend fun supportsClasses(): Boolean {
        classesSupported?.let { return it }

        val session = requireSession()
        val response = client.get("$supabaseUrl/rest/v1/classes") {
            header("apikey", publishableKey)
            header(HttpHeaders.Authorization, "Bearer ${session.accessToken}")
            parameter("select", "id")
            parameter("limit", "1")
        }
        val body = response.bodyAsText()

        if (response.status.isSuccess()) {
            classesSupported = true
            return true
        }

        if (body.contains("classes", ignoreCase = true) && body.contains("does not exist", ignoreCase = true)) {
            classesSupported = false
            return false
        }
        if (body.contains("Could not find the table", ignoreCase = true) && body.contains("classes", ignoreCase = true)) {
            classesSupported = false
            return false
        }

        val message = try {
            json.decodeFromString<ApiError>(body).bestMessage()
        } catch (_: SerializationException) {
            body.ifBlank { "Request failed with ${response.status.value}." }
        }
        throw IllegalStateException(message)
    }

    private suspend fun ensureClassesSupported() {
        if (!supportsClasses()) {
            throw IllegalStateException("Classes management is ready in the app, but the live database has not applied the classes migration yet.")
        }
    }

    private fun classSelectFields(): String {
        return listOf(
            "id",
            "name",
            "institute_id",
            "al_year",
            "monthly_fee",
            "class_type",
            "weekday",
            "start_time",
            "end_time",
            "week_of_month",
            "active_from",
            "active_until",
            "status",
            "notes",
            "created_at",
        ).joinToString(",")
    }

    private fun studentSelectFields(includeMonthlyFee: Boolean): String {
        return buildList {
            add("id")
            add("student_code")
            add("full_name")
            add("al_year")
            add("institute_id")
            if (includeMonthlyFee) {
                add("monthly_fee")
            }
            add("qr_code_id")
            add("whatsapp_number")
            add("qr_link")
            add("joined_date")
            add("status")
        }.joinToString(",")
    }

    private fun normalizeTimeValue(value: String): String {
        val trimmed = value.trim()
        if (trimmed.isBlank()) return "09:00:00"
        return when (trimmed.length) {
            5 -> "$trimmed:00"
            8 -> trimmed
            else -> trimmed
        }
    }

    private fun buildDashboard(
        students: List<Student>,
        attendance: List<AttendanceRecord>,
        payments: List<PaymentRecord>,
    ): DashboardData {
        val today = LocalDate.now()
        val attendanceTrend = (6 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            TrendPoint(
                label = date.format(DateTimeFormatter.ofPattern("MMM dd")),
                value = attendance.count { it.attendanceDate == date.toString() }.toDouble(),
            )
        }
        val monthStarts = (5 downTo 0).map { offset ->
            today.minusMonths(offset.toLong()).with(TemporalAdjusters.firstDayOfMonth())
        }
        val registrationTrend = monthStarts.map { date ->
            TrendPoint(
                label = date.format(DateTimeFormatter.ofPattern("MMM yyyy")),
                value = students.count { it.joinedDate.take(7) == date.toString().take(7) }.toDouble(),
            )
        }
        val incomeTrend = monthStarts.map { date ->
            TrendPoint(
                label = date.format(DateTimeFormatter.ofPattern("MMM yyyy")),
                value = payments
                    .filter { it.paid && it.paymentMonth == date.monthValue && it.paymentYear == date.year }
                    .sumOf { it.amount },
            )
        }
        val paidThisMonth = payments.count {
            it.paid && it.paymentMonth == today.monthValue && it.paymentYear == today.year
        }
        return DashboardData(
            metrics = listOf(
                DashboardMetric("Total students", students.size, "Active roster across your scope"),
                DashboardMetric(
                    "Attendance today",
                    attendance.count { it.attendanceDate == today.toString() },
                    "Students marked from QR or manual entry",
                ),
                DashboardMetric("Paid this month", paidThisMonth, "Monthly fee records already settled"),
                DashboardMetric(
                    "Unpaid this month",
                    (students.size - paidThisMonth).coerceAtLeast(0),
                    "Students still awaiting current month payment",
                ),
            ),
            attendanceTrend = attendanceTrend,
            incomeTrend = incomeTrend,
            registrationTrend = registrationTrend,
        )
    }

    private suspend inline fun <reified T> authorizedRestGetList(
        table: String,
        query: Map<String, String>,
    ): List<T> {
        val session = requireSession()
        val response = client.get("$supabaseUrl/rest/v1/$table") {
            header("apikey", publishableKey)
            header(HttpHeaders.Authorization, "Bearer ${session.accessToken}")
            query.forEach { (key, value) -> parameter(key, value) }
        }
        ensureSuccess(response)
        return response.body()
    }

    private suspend fun authorizedRestPost(table: String, body: Any?) {
        val session = requireSession()
        val response = client.post("$supabaseUrl/rest/v1/$table") {
            header("apikey", publishableKey)
            header(HttpHeaders.Authorization, "Bearer ${session.accessToken}")
            header("Prefer", "return=minimal")
            contentType(ContentType.Application.Json)
            setBody(encodeRequestBody(body))
        }
        ensureSuccess(response)
    }

    private suspend fun authorizedRestPatch(table: String, filters: Map<String, String>, body: Any?) {
        val session = requireSession()
        val response = client.patch("$supabaseUrl/rest/v1/$table") {
            header("apikey", publishableKey)
            header(HttpHeaders.Authorization, "Bearer ${session.accessToken}")
            header("Prefer", "return=minimal")
            contentType(ContentType.Application.Json)
            filters.forEach { (key, value) -> parameter(key, value) }
            setBody(encodeRequestBody(body))
        }
        ensureSuccess(response)
    }

    private suspend fun authorizedRestDelete(table: String, filters: Map<String, String>) {
        val session = requireSession()
        val response = client.delete("$supabaseUrl/rest/v1/$table") {
            header("apikey", publishableKey)
            header(HttpHeaders.Authorization, "Bearer ${session.accessToken}")
            header("Prefer", "return=minimal")
            filters.forEach { (key, value) -> parameter(key, value) }
        }
        ensureSuccess(response)
    }

    private suspend fun authorizedRpc(functionName: String, body: Any?) {
        val session = requireSession()
        val response = client.post("$supabaseUrl/rest/v1/rpc/$functionName") {
            header("apikey", publishableKey)
            header(HttpHeaders.Authorization, "Bearer ${session.accessToken}")
            contentType(ContentType.Application.Json)
            setBody(encodeRequestBody(body))
        }
        ensureSuccess(response)
    }

    private suspend fun requireSession(): StoredSession = sessionMutex.withLock {
        val session = sessionStore.read() ?: throw SessionExpiredException("Please sign in again.")
        if (isSessionPastLimit(session)) {
            sessionStore.clear()
            throw SessionExpiredException("Your 24-hour mobile session has expired. Please sign in again.")
        }
        if (System.currentTimeMillis() + 60_000L >= session.expiresAt) {
            return@withLock refreshSession(session)
        }
        session
    }

    private suspend fun refreshSession(current: StoredSession): StoredSession {
        val response = client.post("$supabaseUrl/auth/v1/token") {
            parameter("grant_type", "refresh_token")
            header("apikey", publishableKey)
            contentType(ContentType.Application.Json)
            setBody(encodeRequestBody(mapOf("refresh_token" to current.refreshToken)))
        }
        ensureSuccess(response)
        val session = response.body<AuthSessionResponse>()
        val refreshed = StoredSession(
            accessToken = session.accessToken,
            refreshToken = session.refreshToken,
            userId = session.user.id,
            loginStartedAt = current.loginStartedAt,
            expiresAt = resolveExpiry(session),
        )
        sessionStore.save(refreshed)
        return refreshed
    }

    private fun isSessionPastLimit(session: StoredSession): Boolean {
        return System.currentTimeMillis() - session.loginStartedAt > BuildConfig.SESSION_MAX_AGE_MS
    }

    private fun resolveExpiry(session: AuthSessionResponse): Long {
        return session.expiresAt?.times(1000L) ?: (System.currentTimeMillis() + session.expiresIn * 1000L)
    }

    private fun encodeRequestBody(body: Any?): String {
        return json.encodeToString(JsonElement.serializer(), body.toJsonElement())
    }

    private fun Map<*, *>.toJsonObject(): JsonObject = buildJsonObject {
        forEach { (key, value) ->
            require(key is String) { "Only string JSON keys are supported." }
            put(key, value.toJsonElement())
        }
    }

    private fun Iterable<*>.toJsonArray(): JsonArray = buildJsonArray {
        forEach { add(it.toJsonElement()) }
    }

    private fun Any?.toJsonElement(): JsonElement {
        return when (this) {
            null -> JsonNull
            is JsonElement -> this
            is String -> JsonPrimitive(this)
            is Number -> JsonPrimitive(this)
            is Boolean -> JsonPrimitive(this)
            is Map<*, *> -> this.toJsonObject()
            is Iterable<*> -> this.toJsonArray()
            is Array<*> -> this.asIterable().toJsonArray()
            is Enum<*> -> JsonPrimitive(this.name)
            else -> JsonPrimitive(this.toString())
        }
    }

    private suspend fun ensureSuccess(response: HttpResponse) {
        if (response.status.isSuccess()) return
        val body = response.bodyAsText()
        val message = try {
            json.decodeFromString<ApiError>(body).bestMessage()
        } catch (_: SerializationException) {
            body.ifBlank { "Request failed with ${response.status.value}." }
        }
        throw IllegalStateException(message)
    }

    override fun close() {
        client.close()
    }
}

@Serializable
private data class ApiError(
    val message: String? = null,
    @SerialName("error_description") val errorDescription: String? = null,
    val details: JsonElement? = null,
) {
    fun bestMessage(): String {
        return errorDescription ?: message ?: details?.toString() ?: "Request failed."
    }
}
