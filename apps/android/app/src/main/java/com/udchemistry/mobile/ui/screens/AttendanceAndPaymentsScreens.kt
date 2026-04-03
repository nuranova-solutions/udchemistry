package com.udchemistry.mobile.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.Checklist
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.udchemistry.mobile.model.AttendanceDraft
import com.udchemistry.mobile.model.AttendanceRecord
import com.udchemistry.mobile.model.PaymentDraft
import com.udchemistry.mobile.ui.MainUiState
import com.udchemistry.mobile.ui.components.ClickableInfoRow
import com.udchemistry.mobile.ui.components.EmptyState
import com.udchemistry.mobile.ui.components.PageHeroCard
import com.udchemistry.mobile.ui.components.SectionCard
import com.udchemistry.mobile.ui.components.StatusChip
import com.udchemistry.mobile.ui.theme.AppAccent
import com.udchemistry.mobile.ui.theme.AppDanger
import com.udchemistry.mobile.ui.theme.AppMutedText
import com.udchemistry.mobile.ui.theme.AppPrimary
import com.udchemistry.mobile.ui.theme.AppSuccess
import com.udchemistry.mobile.util.formatCurrency
import com.udchemistry.mobile.util.formatDate
import java.time.LocalDate

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AttendanceScreen(
    uiState: MainUiState,
    modifier: Modifier = Modifier,
    onScan: (String) -> Unit,
    onMarkPaid: (String) -> Unit,
    onSkipPayment: () -> Unit,
    onDoneScan: () -> Unit,
    onSave: (String?, AttendanceDraft) -> Unit,
    onDelete: (String) -> Unit,
    immersive: Boolean = false,
    onExitImmersive: () -> Unit = {},
) {
    val layout = com.udchemistry.mobile.ui.rememberAdaptiveLayout()
    val context = LocalContext.current
    var editingAttendance by remember { mutableStateOf<AttendanceRecord?>(null) }
    var studentId by rememberSaveable { mutableStateOf(uiState.students.firstOrNull()?.id.orEmpty()) }
    var attendanceDate by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var status by rememberSaveable { mutableStateOf("present") }
    var manualQrValue by rememberSaveable { mutableStateOf("") }
    var dateFilter by rememberSaveable { mutableStateOf("all") }
    var scannerMode by rememberSaveable { mutableStateOf("scan") }
    var lastFeedbackKey by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(uiState.students) {
        if (studentId.isBlank()) {
            studentId = uiState.students.firstOrNull()?.id.orEmpty()
        }
    }

    fun resetAttendanceForm() {
        editingAttendance = null
        studentId = uiState.students.firstOrNull()?.id.orEmpty()
        attendanceDate = LocalDate.now().toString()
        status = "present"
    }

    LaunchedEffect(uiState.notice) {
        if (uiState.notice == "Attendance saved successfully.") {
            resetAttendanceForm()
        }
    }

    val visibleAttendance = remember(uiState.attendance, dateFilter) {
        uiState.attendance.filter { dateFilter == "all" || it.attendanceDate == dateFilter }
    }
    val paidStudentIds = remember(uiState.payments) {
        val today = LocalDate.now()
        uiState.payments
            .filter { it.paid && it.paymentMonth == today.monthValue && it.paymentYear == today.year }
            .map { it.studentId }
            .toSet()
    }
    val currentScanPaid = remember(uiState.scanResult, paidStudentIds) {
        uiState.scanResult?.student?.id?.let { it in paidStudentIds } ?: false
    }

    LaunchedEffect(uiState.scanResult?.student?.id, uiState.scanPaymentStatus, currentScanPaid) {
        val result = uiState.scanResult ?: return@LaunchedEffect
        val feedbackKey = "${result.student.id}:${uiState.scanPaymentStatus}:${currentScanPaid}"
        if (feedbackKey != lastFeedbackKey) {
            lastFeedbackKey = feedbackKey
            triggerScannerFeedback(context, unpaid = !currentScanPaid)
        }
    }

    if (immersive && layout.preferTwoColumns) {
        Row(
            modifier = modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ScannerExperience(
                modifier = Modifier.weight(1.05f),
                uiState = uiState,
                manualQrValue = manualQrValue,
                onManualQrChange = { manualQrValue = it },
                onScan = onScan,
                onMarkPaid = onMarkPaid,
                onSkipPayment = onSkipPayment,
                onDoneScan = {
                    manualQrValue = ""
                    onDoneScan()
                },
                onOpenRecords = {},
                onExit = onExitImmersive,
                isCurrentMonthPaid = currentScanPaid,
                showModeActions = false,
            )
            AttendanceRecordsPanel(
                modifier = Modifier.weight(0.95f),
                uiState = uiState,
                editingAttendance = editingAttendance,
                studentId = studentId,
                attendanceDate = attendanceDate,
                status = status,
                dateFilter = dateFilter,
                visibleAttendance = visibleAttendance,
                onStudentIdChange = { studentId = it },
                onAttendanceDateChange = { attendanceDate = it },
                onStatusChange = { status = it },
                onDateFilterChange = { dateFilter = it },
                onEditAttendance = { attendance ->
                    editingAttendance = attendance
                    studentId = attendance.studentId
                    attendanceDate = attendance.attendanceDate
                    status = attendance.status
                },
                onReset = ::resetAttendanceForm,
                onSave = { onSave(editingAttendance?.id, AttendanceDraft(studentId, attendanceDate, status)) },
                onDelete = onDelete,
            )
        }
        return
    }

    if (immersive && scannerMode == "scan") {
        ScannerExperience(
            modifier = modifier.fillMaxSize(),
            uiState = uiState,
            manualQrValue = manualQrValue,
            onManualQrChange = { manualQrValue = it },
            onScan = onScan,
            onMarkPaid = onMarkPaid,
            onSkipPayment = onSkipPayment,
            onDoneScan = {
                manualQrValue = ""
                onDoneScan()
            },
            onOpenRecords = { scannerMode = "records" },
            onExit = onExitImmersive,
            isCurrentMonthPaid = currentScanPaid,
            showModeActions = true,
        )
        return
    }

    AttendanceRecordsPanel(
        modifier = modifier.fillMaxSize(),
        uiState = uiState,
        editingAttendance = editingAttendance,
        studentId = studentId,
        attendanceDate = attendanceDate,
        status = status,
        dateFilter = dateFilter,
        visibleAttendance = visibleAttendance,
        onStudentIdChange = { studentId = it },
        onAttendanceDateChange = { attendanceDate = it },
        onStatusChange = { status = it },
        onDateFilterChange = { dateFilter = it },
        onEditAttendance = { attendance ->
            editingAttendance = attendance
            studentId = attendance.studentId
            attendanceDate = attendance.attendanceDate
            status = attendance.status
        },
        onReset = ::resetAttendanceForm,
        onSave = { onSave(editingAttendance?.id, AttendanceDraft(studentId, attendanceDate, status)) },
        onDelete = onDelete,
        onBackToScan = if (immersive) {
            { scannerMode = "scan" }
        } else {
            null
        },
    )
}

@Composable
private fun ScannerExperience(
    uiState: MainUiState,
    manualQrValue: String,
    onManualQrChange: (String) -> Unit,
    onScan: (String) -> Unit,
    onMarkPaid: (String) -> Unit,
    onSkipPayment: () -> Unit,
    onDoneScan: () -> Unit,
    onOpenRecords: () -> Unit,
    onExit: () -> Unit,
    isCurrentMonthPaid: Boolean,
    showModeActions: Boolean,
    modifier: Modifier = Modifier,
) {
    val scanGlow = if (uiState.scanResult == null) Color.Transparent else if (isCurrentMonthPaid) AppSuccess else AppDanger
    val laserTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "scannerLaser")
    val laserProgress by laserTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(1600, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
        ),
        label = "laserProgress",
    )

    Box(modifier = modifier) {
        CameraScannerPreview(
            modifier = Modifier.fillMaxSize(),
            enabled = !uiState.busy,
            onDetected = onScan,
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.30f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.48f),
                        ),
                    ),
                ),
        )

        if (scanGlow != Color.Transparent) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.radialGradient(
                            colors = listOf(scanGlow.copy(alpha = 0.22f), Color.Transparent),
                            radius = 520f,
                        ),
                    ),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.Black.copy(alpha = 0.42f),
                border = BorderStroke(0.7.dp, Color.White.copy(alpha = 0.12f)),
                modifier = Modifier.clickable(onClick = onExit),
            ) {
                Text(
                    "Close",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            if (showModeActions) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.Black.copy(alpha = 0.42f),
                    border = BorderStroke(0.7.dp, Color.White.copy(alpha = 0.12f)),
                    modifier = Modifier.clickable(onClick = onOpenRecords),
                ) {
                    Text(
                        "Records",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.72f)
                .height(260.dp),
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(32.dp),
                color = Color.White.copy(alpha = 0.05f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f)),
            ) {}

            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 4.dp.toPx()
                val cornerLength = 42.dp.toPx()
                val glowY = size.height * laserProgress
                val left = 18.dp.toPx()
                val top = 18.dp.toPx()
                val right = size.width - 18.dp.toPx()
                val bottom = size.height - 18.dp.toPx()
                val cornerColor = AppPrimary

                drawLine(cornerColor, Offset(left, top + cornerLength), Offset(left, top), strokeWidth)
                drawLine(cornerColor, Offset(left, top), Offset(left + cornerLength, top), strokeWidth)
                drawLine(cornerColor, Offset(right - cornerLength, top), Offset(right, top), strokeWidth)
                drawLine(cornerColor, Offset(right, top), Offset(right, top + cornerLength), strokeWidth)
                drawLine(cornerColor, Offset(left, bottom - cornerLength), Offset(left, bottom), strokeWidth)
                drawLine(cornerColor, Offset(left, bottom), Offset(left + cornerLength, bottom), strokeWidth)
                drawLine(cornerColor, Offset(right - cornerLength, bottom), Offset(right, bottom), strokeWidth)
                drawLine(cornerColor, Offset(right, bottom - cornerLength), Offset(right, bottom), strokeWidth)

                drawLine(
                    color = AppSuccess,
                    start = Offset(left + 12.dp.toPx(), glowY),
                    end = Offset(right - 12.dp.toPx(), glowY),
                    strokeWidth = 3.dp.toPx(),
                )
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 18.dp),
            shape = RoundedCornerShape(28.dp),
            color = Color.Black.copy(alpha = 0.50f),
            border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.14f)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("QR Scan Page", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                Text(
                    if (uiState.scanResult == null) "Keep the student QR inside the reticle for instant validation." else "Student detected. Keep the camera live and finish the next action below.",
                    color = AppMutedText,
                )

                if (uiState.scanResult == null) {
                    OutlinedTextField(
                        value = manualQrValue,
                        onValueChange = onManualQrChange,
                        label = { Text("Manual student ID") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = { onScan(manualQrValue) },
                        enabled = manualQrValue.isNotBlank() && !uiState.busy,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (uiState.busy) "Scanning..." else "Validate manually")
                    }
                } else {
                    val student = uiState.scanResult.student
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            modifier = Modifier.size(56.dp),
                            shape = RoundedCornerShape(18.dp),
                            color = AppPrimary.copy(alpha = 0.22f),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    student.fullName.firstOrNull()?.uppercase() ?: "?",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(student.fullName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(student.whatsappNumber, color = AppMutedText, style = MaterialTheme.typography.bodySmall)
                        }
                        StatusChip(
                            label = if (isCurrentMonthPaid) "Paid" else "Pending",
                            positive = isCurrentMonthPaid,
                        )
                        StatusChip(
                            label = if (student.status == "active") "Active" else "Inactive",
                            positive = student.status == "active",
                        )
                    }

                    Text(
                        if (uiState.scanResult.duplicate) "Attendance already exists for today." else "Attendance marked successfully.",
                        color = if (uiState.scanResult.duplicate) AppMutedText else AppSuccess,
                    )

                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onMarkPaid(student.id) },
                            enabled = uiState.scanPaymentStatus != "paid",
                        ) {
                            Text(if (uiState.scanPaymentStatus == "paid") "Paid" else "Mark Paid")
                        }
                        TextButton(onClick = onSkipPayment) {
                            Text(if (uiState.scanPaymentStatus == "skipped") "Skipped" else "Skip")
                        }
                        TextButton(onClick = onDoneScan) {
                            Text("Done")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AttendanceRecordsPanel(
    uiState: MainUiState,
    editingAttendance: AttendanceRecord?,
    studentId: String,
    attendanceDate: String,
    status: String,
    dateFilter: String,
    visibleAttendance: List<AttendanceRecord>,
    onStudentIdChange: (String) -> Unit,
    onAttendanceDateChange: (String) -> Unit,
    onStatusChange: (String) -> Unit,
    onDateFilterChange: (String) -> Unit,
    onEditAttendance: (AttendanceRecord) -> Unit,
    onReset: () -> Unit,
    onSave: () -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
    onBackToScan: (() -> Unit)? = null,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            if (onBackToScan != null) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onBackToScan) { Text("Back to scanner") }
                }
            }
        }

        item {
            PageHeroCard(
                eyebrow = "Attendance control",
                title = "Scan fast, correct faster",
                description = "Keep the QR-first flow quick while still giving staff a clean place to fix mistakes and review daily records.",
                icon = Icons.Outlined.Checklist,
                accentColor = AppPrimary,
                highlights = listOf(
                    "${uiState.attendance.size} records",
                    "${visibleAttendance.size} in view",
                    if (onBackToScan != null) "Scanner linked" else "Manual mode ready",
                ),
            )
        }

        item {
            SectionCard(
                title = if (editingAttendance == null) "Attendance editor" else "Edit attendance",
                description = "Correct a scan or add a manual attendance mark without leaving the premium scan workflow.",
            ) {
                FilterSelector(
                    label = "Student",
                    selected = studentId,
                    options = uiState.students.map { it.id to it.fullName },
                    onSelected = onStudentIdChange,
                )
                OutlinedTextField(
                    value = attendanceDate,
                    onValueChange = onAttendanceDateChange,
                    label = { Text("Date (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                FilterSelector(
                    label = "Status",
                    selected = status,
                    options = listOf("present" to "Present", "late" to "Late", "absent" to "Absent"),
                    onSelected = onStatusChange,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onSave, enabled = studentId.isNotBlank() && !uiState.busy) {
                        Text(if (uiState.busy) "Saving..." else "Save")
                    }
                    TextButton(onClick = onReset) { Text("Clear") }
                }
            }
        }

        item {
            SectionCard(
                title = "Attendance records",
                description = "Review recent attendance, filter by date, then edit or delete entries.",
            ) {
                FilterSelector(
                    label = "Date filter",
                    selected = dateFilter,
                    options = listOf("all" to "All dates") + uiState.attendance.map { it.attendanceDate to formatDate(it.attendanceDate) }.distinct(),
                    onSelected = onDateFilterChange,
                )
                if (visibleAttendance.isEmpty()) {
                    EmptyState("No attendance records found.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        visibleAttendance.forEach { attendance ->
                            val studentName = uiState.students.firstOrNull { it.id == attendance.studentId }?.fullName ?: "Unknown student"
                            ClickableInfoRow(
                                headline = studentName,
                                supporting = "${formatDate(attendance.attendanceDate)} - ${formatDate(attendance.markedAt)}",
                                trailing = { StatusChip(attendance.status, attendance.status == "present") },
                                onClick = { onEditAttendance(attendance) },
                            )
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                TextButton(onClick = { onEditAttendance(attendance) }) { Text("Edit") }
                                TextButton(onClick = { onDelete(attendance.id) }) { Text("Delete") }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun triggerScannerFeedback(context: android.content.Context, unpaid: Boolean) {
    val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        val manager = context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
        manager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator
    }

    if (!vibrator.hasVibrator()) return

    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
        val effect = if (unpaid) {
            android.os.VibrationEffect.createOneShot(220L, android.os.VibrationEffect.DEFAULT_AMPLITUDE)
        } else {
            android.os.VibrationEffect.createWaveform(longArrayOf(0L, 35L, 55L, 40L), -1)
        }
        vibrator.vibrate(effect)
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(if (unpaid) 220L else 100L)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PaymentsScreen(
    uiState: MainUiState,
    modifier: Modifier = Modifier,
    onSave: (String?, PaymentDraft) -> Unit,
    onDelete: (String) -> Unit,
) {
    var editingPaymentId by rememberSaveable { mutableStateOf<String?>(null) }
    var studentId by rememberSaveable { mutableStateOf(uiState.students.firstOrNull()?.id.orEmpty()) }
    var month by rememberSaveable { mutableStateOf(LocalDate.now().monthValue.toString()) }
    var year by rememberSaveable { mutableStateOf(LocalDate.now().year.toString()) }
    fun editableMoney(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.toLong().toString()
        } else {
            value.toString()
        }
    }
    fun monthlyFeeForStudent(selectedStudentId: String): String {
        val monthlyFee = uiState.students.firstOrNull { it.id == selectedStudentId }?.monthlyFee ?: 0.0
        return editableMoney(monthlyFee)
    }

    var amount by rememberSaveable { mutableStateOf(monthlyFeeForStudent(studentId)) }
    var paid by rememberSaveable { mutableStateOf("paid") }
    var paidDate by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var monthFilter by rememberSaveable { mutableStateOf("all") }
    var yearFilter by rememberSaveable { mutableStateOf("all") }

    fun reset() {
        editingPaymentId = null
        studentId = uiState.students.firstOrNull()?.id.orEmpty()
        month = LocalDate.now().monthValue.toString()
        year = LocalDate.now().year.toString()
        amount = monthlyFeeForStudent(studentId)
        paid = "paid"
        paidDate = LocalDate.now().toString()
    }

    LaunchedEffect(uiState.students) {
        if (studentId.isBlank()) {
            studentId = uiState.students.firstOrNull()?.id.orEmpty()
            if (editingPaymentId == null) {
                amount = monthlyFeeForStudent(studentId)
            }
        }
    }

    LaunchedEffect(uiState.notice) {
        if (uiState.notice == "Payment saved successfully.") {
            reset()
        }
    }

    val visiblePayments = remember(uiState.payments, monthFilter, yearFilter) {
        uiState.payments.filter { payment ->
            (monthFilter == "all" || payment.paymentMonth.toString() == monthFilter) &&
                (yearFilter == "all" || payment.paymentYear.toString() == yearFilter)
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            val currentMonthPaid = uiState.payments.count {
                it.paid &&
                    it.paymentMonth == LocalDate.now().monthValue &&
                    it.paymentYear == LocalDate.now().year
            }
            PageHeroCard(
                eyebrow = "Collections",
                title = "Payments and monthly dues",
                description = "Review fees, correct records, and keep unpaid students visible without turning the workflow into accounting friction.",
                icon = Icons.Outlined.AttachMoney,
                accentColor = AppAccent,
                highlights = listOf(
                    "${visiblePayments.size} filtered records",
                    "$currentMonthPaid paid this month",
                    "${uiState.students.size} active students",
                ),
            )
        }

        item {
            SectionCard(
                title = if (editingPaymentId == null) "Add payment" else "Edit payment",
                description = "Mark monthly fee records or correct them manually from Android.",
            ) {
                FilterSelector(
                    label = "Student",
                    selected = studentId,
                    options = uiState.students.map { it.id to it.fullName },
                    onSelected = { selectedId ->
                        studentId = selectedId
                        if (editingPaymentId == null) {
                            amount = monthlyFeeForStudent(selectedId)
                        }
                    },
                )
                OutlinedTextField(value = month, onValueChange = { month = it.filter(Char::isDigit) }, label = { Text("Month") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = year, onValueChange = { year = it.filter(Char::isDigit) }, label = { Text("Year") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount") }, modifier = Modifier.fillMaxWidth())
                FilterSelector(
                    label = "Payment status",
                    selected = paid,
                    options = listOf("paid" to "Paid", "unpaid" to "Unpaid"),
                    onSelected = { paid = it },
                )
                OutlinedTextField(value = paidDate, onValueChange = { paidDate = it }, label = { Text("Paid date (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            onSave(
                                editingPaymentId,
                                PaymentDraft(
                                    studentId = studentId,
                                    paymentMonth = month.toIntOrNull() ?: LocalDate.now().monthValue,
                                    paymentYear = year.toIntOrNull() ?: LocalDate.now().year,
                                    amount = amount.toDoubleOrNull() ?: 0.0,
                                    paid = paid == "paid",
                                    paidDate = if (paid == "paid") paidDate else null,
                                ),
                            )
                        },
                        enabled = studentId.isNotBlank() && !uiState.busy,
                    ) {
                        Text(if (uiState.busy) "Saving..." else "Save")
                    }
                    TextButton(onClick = ::reset) { Text("Clear") }
                }
            }
        }

        item {
            SectionCard(title = "Payment records", description = "Filter monthly records, then edit or remove entries.") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterSelector(
                        label = "Month filter",
                        selected = monthFilter,
                        options = listOf("all" to "All months") + uiState.payments.map { it.paymentMonth.toString() to "Month ${it.paymentMonth}" }.distinct(),
                        onSelected = { monthFilter = it },
                    )
                    FilterSelector(
                        label = "Year filter",
                        selected = yearFilter,
                        options = listOf("all" to "All years") + uiState.payments.map { it.paymentYear.toString() to it.paymentYear.toString() }.distinct(),
                        onSelected = { yearFilter = it },
                    )
                }
                if (visiblePayments.isEmpty()) {
                    EmptyState("No payment records found.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        visiblePayments.forEach { payment ->
                            val studentName = uiState.students.firstOrNull { it.id == payment.studentId }?.fullName ?: "Unknown student"
                            ClickableInfoRow(
                                headline = studentName,
                                supporting = "${payment.paymentMonth}/${payment.paymentYear} - ${formatCurrency(payment.amount)}\nPaid date: ${formatDate(payment.paidDate)}",
                                trailing = { StatusChip(if (payment.paid) "Paid" else "Unpaid", payment.paid) },
                                onClick = {
                                    editingPaymentId = payment.id
                                    studentId = payment.studentId
                                    month = payment.paymentMonth.toString()
                                    year = payment.paymentYear.toString()
                                    amount = payment.amount.toString()
                                    paid = if (payment.paid) "paid" else "unpaid"
                                    paidDate = payment.paidDate.orEmpty()
                                },
                            )
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                TextButton(onClick = {
                                    editingPaymentId = payment.id
                                    studentId = payment.studentId
                                    month = payment.paymentMonth.toString()
                                    year = payment.paymentYear.toString()
                                    amount = payment.amount.toString()
                                    paid = if (payment.paid) "paid" else "unpaid"
                                    paidDate = payment.paidDate.orEmpty()
                                }) { Text("Edit") }
                                TextButton(onClick = { onDelete(payment.id) }) { Text("Delete") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraScannerPreview(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    onDetected: (String) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var permissionGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var lastValue by remember { mutableStateOf("") }
    var lastTimestamp by remember { mutableLongStateOf(0L) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permissionGranted = it
    }

    LaunchedEffect(Unit) {
        if (!permissionGranted) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (!permissionGranted) {
        EmptyState("Allow camera access to scan student QR codes.")
        return
    }

    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    DisposableEffect(enabled, permissionGranted) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val executor = ContextCompat.getMainExecutor(context)
        val barcodeScanner = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build(),
        )

        val listener = Runnable {
            val cameraProvider = cameraProviderFuture.get()
            cameraProvider.unbindAll()
            if (!enabled) {
                return@Runnable
            }

            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }

            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { useCase ->
                    useCase.setAnalyzer(executor) { imageProxy ->
                        val mediaImage = imageProxy.image
                        if (mediaImage == null) {
                            imageProxy.close()
                            return@setAnalyzer
                        }

                        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                        barcodeScanner.process(image)
                            .addOnSuccessListener { barcodes ->
                                val detected = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }?.rawValue.orEmpty()
                                if (detected.isNotBlank()) {
                                    val now = System.currentTimeMillis()
                                    if (detected != lastValue || now - lastTimestamp > 2_500L) {
                                        lastValue = detected
                                        lastTimestamp = now
                                        onDetected(detected)
                                    }
                                }
                            }
                            .addOnCompleteListener { imageProxy.close() }
                    }
                }

            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                analysis,
            )
        }

        cameraProviderFuture.addListener(listener, executor)

        onDispose {
            runCatching {
                cameraProviderFuture.get().unbindAll()
            }
            barcodeScanner.close()
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier,
    )
}
