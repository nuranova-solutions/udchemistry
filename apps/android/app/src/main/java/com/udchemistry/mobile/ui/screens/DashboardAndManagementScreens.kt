package com.udchemistry.mobile.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PeopleAlt
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SupervisorAccount
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.udchemistry.mobile.model.AttendanceRecord
import com.udchemistry.mobile.model.Institute
import com.udchemistry.mobile.model.InstituteDraft
import com.udchemistry.mobile.model.PaymentRecord
import com.udchemistry.mobile.model.QrCodeRecord
import com.udchemistry.mobile.model.Role
import com.udchemistry.mobile.model.StaffDraft
import com.udchemistry.mobile.model.Student
import com.udchemistry.mobile.model.StudentDraft
import com.udchemistry.mobile.ui.MainUiState
import com.udchemistry.mobile.ui.components.ClickableInfoRow
import com.udchemistry.mobile.ui.components.EmptyState
import com.udchemistry.mobile.ui.components.PageHeroCard
import com.udchemistry.mobile.ui.components.QrPreviewDialog
import com.udchemistry.mobile.ui.components.SectionCard
import com.udchemistry.mobile.ui.components.StatusChip
import com.udchemistry.mobile.ui.components.TrendChart
import com.udchemistry.mobile.ui.theme.AppAccent
import com.udchemistry.mobile.ui.theme.AppAccentSoft
import com.udchemistry.mobile.ui.theme.AppDivider
import com.udchemistry.mobile.ui.theme.AppMutedText
import com.udchemistry.mobile.ui.theme.AppPrimary
import com.udchemistry.mobile.ui.theme.AppSuccess
import com.udchemistry.mobile.ui.theme.AppSurfaceMuted
import com.udchemistry.mobile.util.ExportHelper
import com.udchemistry.mobile.util.attendanceForMonth
import com.udchemistry.mobile.util.buildStudentAttendanceTrend
import com.udchemistry.mobile.util.buildStudentMonthOptions
import com.udchemistry.mobile.util.buildStudentPaymentTrend
import com.udchemistry.mobile.util.countAttendedClasses
import com.udchemistry.mobile.util.currentMonthKey
import com.udchemistry.mobile.util.formatCurrency
import com.udchemistry.mobile.util.formatDate
import com.udchemistry.mobile.util.formatMonthKeyLabel
import com.udchemistry.mobile.util.formatShortAttendanceDate
import com.udchemistry.mobile.util.paymentsForMonth
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun DashboardScreen(
    uiState: MainUiState,
    modifier: Modifier = Modifier,
    onOpenReports: () -> Unit = {},
    onOpenStudents: () -> Unit = {},
    onOpenClasses: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    onOpenAttendance: () -> Unit = {},
) {
    val layout = com.udchemistry.mobile.ui.rememberAdaptiveLayout()
    val today = remember { LocalDate.now() }
    val todayIso = today.toString()
    val incomeThisMonth = remember(uiState.payments) {
        uiState.payments
            .filter { it.paid && it.paymentMonth == today.monthValue && it.paymentYear == today.year }
            .sumOf { it.amount }
    }
    val paidStudentIds = remember(uiState.payments) {
        uiState.payments
            .filter { it.paid && it.paymentMonth == today.monthValue && it.paymentYear == today.year }
            .map { it.studentId }
            .toSet()
    }
    val attendanceToday = remember(uiState.attendance) {
        uiState.attendance.count { record ->
            record.attendanceDate == todayIso && record.status.lowercase() != "absent"
        }
    }
    val unpaidThisMonth = remember(uiState.students, paidStudentIds) {
        uiState.students.count { it.id !in paidStudentIds }
    }
    val monthLabel = remember(today) {
        today.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
    }
    val attendanceProgress = remember(attendanceToday, uiState.students.size) {
        if (uiState.students.isEmpty()) 0f else attendanceToday.toFloat() / uiState.students.size.toFloat()
    }
    val showRevenueCard = uiState.profile?.role != Role.Staff

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(layout.contentSpacing),
    ) {
        item {
            DashboardHeaderCard()
        }

        item {
            if (showRevenueCard) {
                if (layout.preferTwoColumns) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(layout.contentSpacing),
                    ) {
                        RevenueHeroCard(
                            modifier = Modifier.weight(1.35f),
                            monthLabel = monthLabel,
                            amount = incomeThisMonth,
                            points = uiState.dashboard.incomeTrend,
                        )
                        AttendanceRingCard(
                            modifier = Modifier.weight(1f),
                            attendanceToday = attendanceToday,
                            totalStudents = uiState.students.size,
                            progress = attendanceProgress,
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(layout.contentSpacing)) {
                        RevenueHeroCard(
                            monthLabel = monthLabel,
                            amount = incomeThisMonth,
                            points = uiState.dashboard.incomeTrend,
                        )
                        AttendanceRingCard(
                            attendanceToday = attendanceToday,
                            totalStudents = uiState.students.size,
                            progress = attendanceProgress,
                        )
                    }
                }
            } else {
                AttendanceRingCard(
                    modifier = Modifier.fillMaxWidth(),
                    attendanceToday = attendanceToday,
                    totalStudents = uiState.students.size,
                    progress = attendanceProgress,
                )
            }
        }

        item {
            if (layout.preferTwoColumns) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(layout.contentSpacing),
                ) {
                    TrendCenterCard(
                        modifier = Modifier.weight(1.4f),
                        points = if (uiState.dashboard.incomeTrend.isNotEmpty()) uiState.dashboard.incomeTrend else uiState.dashboard.attendanceTrend,
                    )
                    DashboardQuickActionCluster(
                        modifier = Modifier.weight(1f),
                        onOpenReports = onOpenReports,
                        onOpenStudents = onOpenStudents,
                        onOpenClasses = onOpenClasses,
                        onOpenProfile = onOpenProfile,
                        onOpenAttendance = onOpenAttendance,
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(layout.contentSpacing)) {
                    TrendCenterCard(
                        points = if (uiState.dashboard.incomeTrend.isNotEmpty()) uiState.dashboard.incomeTrend else uiState.dashboard.attendanceTrend,
                    )
                    DashboardQuickActionCluster(
                        onOpenReports = onOpenReports,
                        onOpenStudents = onOpenStudents,
                        onOpenClasses = onOpenClasses,
                        onOpenProfile = onOpenProfile,
                        onOpenAttendance = onOpenAttendance,
                    )
                }
            }
        }

        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DashboardStatCard(
                    title = "Total Students",
                    value = uiState.students.size.toDouble(),
                    formatter = { it.toInt().toString() },
                    accentColor = AppPrimary,
                )
                DashboardStatCard(
                    title = "Attendance Today",
                    value = attendanceToday.toDouble(),
                    formatter = { it.toInt().toString().padStart(2, '0') },
                    accentColor = AppSuccess,
                )
                DashboardStatCard(
                    title = "Unpaid This Month",
                    value = unpaidThisMonth.toDouble(),
                    formatter = { it.toInt().toString().padStart(2, '0') },
                    accentColor = AppAccent,
                )
            }
        }

        if (uiState.dashboard.registrationTrend.isNotEmpty()) {
            item {
                TrendChart(
                    title = "Growth Flow",
                    subtitle = "Registration momentum across recent months.",
                    points = uiState.dashboard.registrationTrend,
                    barMode = true,
                )
            }
        }

        if (uiState.dashboard.attendanceTrend.isNotEmpty()) {
            item {
                TrendChart(
                    title = "Attendance Pulse",
                    subtitle = "Recent daily attendance activity across the institute.",
                    points = uiState.dashboard.attendanceTrend,
                )
            }
        }
    }
}

@Composable
private fun DashboardHeaderCard() {
    com.udchemistry.mobile.ui.components.GlassPanel(
        glowColor = AppPrimary,
        shape = RoundedCornerShape(30.dp),
    ) {
        Text("Active dashboard", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
        Text(
            "Track revenue, live attendance, unpaid students, and the scan-first flow from one premium control center.",
            color = AppMutedText,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun RevenueHeroCard(
    monthLabel: String,
    amount: Double,
    points: List<com.udchemistry.mobile.model.TrendPoint>,
    modifier: Modifier = Modifier,
) {
    com.udchemistry.mobile.ui.components.GlassPanel(
        modifier = modifier,
        glowColor = AppSuccess,
        shape = RoundedCornerShape(32.dp),
    ) {
        Text("Total Revenue", color = AppMutedText, style = MaterialTheme.typography.labelLarge)
        AnimatedMetricValue(
            targetValue = amount,
            formatter = { formatCurrency(it.toDouble()) },
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(monthLabel, color = AppMutedText, style = MaterialTheme.typography.titleMedium)
        Sparkline(points = points.ifEmpty { listOf(com.udchemistry.mobile.model.TrendPoint(monthLabel, amount)) })
    }
}

@Composable
private fun AttendanceRingCard(
    attendanceToday: Int,
    totalStudents: Int,
    progress: Float,
    modifier: Modifier = Modifier,
) {
    com.udchemistry.mobile.ui.components.GlassPanel(
        modifier = modifier,
        glowColor = AppPrimary,
        shape = RoundedCornerShape(32.dp),
    ) {
        Text("Attendance Today", color = AppMutedText, style = MaterialTheme.typography.labelLarge)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(180.dp)) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.08f),
                    style = Stroke(width = 18.dp.toPx()),
                )
                drawArc(
                    color = AppPrimary,
                    startAngle = -90f,
                    sweepAngle = 360f * progress.coerceIn(0f, 1f),
                    useCenter = false,
                    style = Stroke(width = 18.dp.toPx()),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AnimatedMetricValue(
                    targetValue = attendanceToday.toDouble(),
                    formatter = { it.toInt().toString() },
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text("Present now", color = AppMutedText)
            }
        }
        Text("$attendanceToday of $totalStudents students marked today", color = AppMutedText)
    }
}

@Composable
private fun TrendCenterCard(
    points: List<com.udchemistry.mobile.model.TrendPoint>,
    modifier: Modifier = Modifier,
) {
    val safePoints = remember(points) { points.takeLast(7) }
    val latestPoint = safePoints.lastOrNull()

    com.udchemistry.mobile.ui.components.GlassPanel(
        modifier = modifier,
        glowColor = AppPrimary,
        shape = RoundedCornerShape(32.dp),
    ) {
        Text("Trend Center", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Recent activity curve with the latest highlighted value.", color = AppMutedText)

        if (safePoints.isEmpty()) {
            EmptyState("No trend data available yet.")
        } else {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.White.copy(alpha = 0.06f),
            ) {
                Text(
                    "${formatCurrency((latestPoint?.value ?: 0.0))}  |  ${latestPoint?.label.orEmpty()}",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            TrendCenterChart(points = safePoints)
        }
    }
}

@Composable
private fun DashboardQuickActionCluster(
    onOpenReports: () -> Unit,
    onOpenStudents: () -> Unit,
    onOpenClasses: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenAttendance: () -> Unit,
    modifier: Modifier = Modifier,
) {
    com.udchemistry.mobile.ui.components.GlassPanel(
        modifier = modifier,
        shape = RoundedCornerShape(32.dp),
    ) {
        Text("Action Hub", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Jump to the most-used flows with one tap.", color = AppMutedText)

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DashboardShortcutCard("Summary", Icons.Outlined.Assessment, onOpenReports)
            DashboardShortcutCard("Students", Icons.Outlined.PeopleAlt, onOpenStudents)
            DashboardShortcutCard("Classes", Icons.AutoMirrored.Outlined.MenuBook, onOpenClasses)
            DashboardShortcutCard("Settings", Icons.Outlined.Settings, onOpenProfile)
            DashboardShortcutCard("Scanner", Icons.Outlined.QrCodeScanner, onOpenAttendance)
        }
    }
}

@Composable
private fun DashboardShortcutCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .size(width = 140.dp, height = 138.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(alpha = 0.05f),
        border = BorderStroke(0.7.dp, AppDivider),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
        ) {
            Surface(
                modifier = Modifier.size(54.dp),
                shape = RoundedCornerShape(18.dp),
                color = AppAccentSoft,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = title, tint = AppAccent)
                }
            }
            Text(title, style = MaterialTheme.typography.titleMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
private fun DashboardStatCard(
    title: String,
    value: Double,
    formatter: (Float) -> String,
    accentColor: Color,
) {
    com.udchemistry.mobile.ui.components.GlassPanel(
        modifier = Modifier.widthIn(min = 170.dp),
        shape = RoundedCornerShape(28.dp),
        glowColor = accentColor,
    ) {
        AnimatedMetricValue(
            targetValue = value,
            formatter = formatter,
            style = MaterialTheme.typography.displayMedium,
            color = accentColor,
        )
        Text(title, color = AppMutedText, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun AnimatedMetricValue(
    targetValue: Double,
    formatter: (Float) -> String,
    style: androidx.compose.ui.text.TextStyle,
    color: Color,
) {
    val animatedValue by androidx.compose.animation.core.animateFloatAsState(
        targetValue = targetValue.toFloat(),
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 1100),
        label = "dashboardMetric",
    )
    Text(
        formatter(animatedValue),
        style = style,
        color = color,
        fontWeight = FontWeight.ExtraBold,
    )
}

@Composable
private fun Sparkline(points: List<com.udchemistry.mobile.model.TrendPoint>) {
    val safePoints = remember(points) { points.takeLast(7) }
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 88.dp),
    ) {
        val maxValue = (safePoints.maxOfOrNull { it.value } ?: 1.0).coerceAtLeast(1.0)
        val minValue = safePoints.minOfOrNull { it.value } ?: 0.0
        val range = (maxValue - minValue).coerceAtLeast(1.0)
        val startX = 8.dp.toPx()
        val endX = size.width - 8.dp.toPx()
        val bottom = size.height - 10.dp.toPx()
        val top = 10.dp.toPx()
        val stepX = if (safePoints.size > 1) (endX - startX) / (safePoints.size - 1) else 0f
        val path = Path()

        safePoints.forEachIndexed { index, point ->
            val x = startX + stepX * index
            val normalized = ((point.value - minValue) / range).toFloat()
            val y = bottom - ((bottom - top) * normalized)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(path = path, color = AppSuccess, style = Stroke(width = 4.dp.toPx()))
        safePoints.forEachIndexed { index, point ->
            val x = startX + stepX * index
            val normalized = ((point.value - minValue) / range).toFloat()
            val y = bottom - ((bottom - top) * normalized)
            drawCircle(color = AppSuccess, radius = 4.dp.toPx(), center = Offset(x, y))
        }
    }
}

@Composable
private fun TrendCenterChart(points: List<com.udchemistry.mobile.model.TrendPoint>) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 240.dp),
    ) {
        val maxValue = (points.maxOfOrNull { it.value } ?: 1.0).coerceAtLeast(1.0)
        val minValue = points.minOfOrNull { it.value } ?: 0.0
        val range = (maxValue - minValue).coerceAtLeast(1.0)
        val left = 16.dp.toPx()
        val right = size.width - 16.dp.toPx()
        val top = 14.dp.toPx()
        val bottom = size.height - 24.dp.toPx()
        val stepX = if (points.size > 1) (right - left) / (points.size - 1) else 0f
        val path = Path()
        val fill = Path()

        points.forEachIndexed { index, point ->
            val x = left + stepX * index
            val normalized = ((point.value - minValue) / range).toFloat()
            val y = bottom - ((bottom - top) * normalized)
            if (index == 0) {
                path.moveTo(x, y)
                fill.moveTo(x, bottom)
                fill.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fill.lineTo(x, y)
            }
        }

        fill.lineTo(right, bottom)
        fill.close()

        drawPath(
            path = fill,
            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                colors = listOf(AppPrimary.copy(alpha = 0.34f), Color.Transparent),
                startY = top,
                endY = bottom,
            ),
        )
        drawPath(path = path, color = AppPrimary, style = Stroke(width = 5.dp.toPx()))
        points.forEachIndexed { index, point ->
            val x = left + stepX * index
            val normalized = ((point.value - minValue) / range).toFloat()
            val y = bottom - ((bottom - top) * normalized)
            drawCircle(color = AppPrimary, radius = 6.dp.toPx(), center = Offset(x, y))
        }
    }
}

@Composable
fun InstitutesScreen(
    uiState: MainUiState,
    modifier: Modifier = Modifier,
    onSave: (String?, InstituteDraft) -> Unit,
    onDelete: (String) -> Unit,
) {
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var name by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var contactNo by rememberSaveable { mutableStateOf("") }
    var status by rememberSaveable { mutableStateOf("active") }

    fun reset() {
        editingId = null
        name = ""
        code = ""
        address = ""
        contactNo = ""
        status = "active"
    }

    LaunchedEffect(uiState.notice) {
        if (uiState.notice == "Institute saved successfully.") {
            reset()
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            PageHeroCard(
                eyebrow = "Admin workspace",
                title = "Institute network",
                description = "Keep campus records clean, searchable, and ready for staff assignment across every screen size.",
                icon = Icons.Outlined.School,
                accentColor = AppPrimary,
                highlights = listOf(
                    "${uiState.institutes.size} institutes",
                    "Cloud synced",
                    "Admin only",
                ),
            )
        }

        item {
            SectionCard(
                title = if (editingId == null) "Add institute" else "Edit institute",
                description = "Admins can add, edit, and delete institutes directly from Android.",
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Institute name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Code") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = contactNo, onValueChange = { contactNo = it }, label = { Text("Contact number") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth())
                StatusSelector(selected = status, onSelected = { status = it })
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            onSave(editingId, InstituteDraft(name, code, address, contactNo, status))
                        },
                        enabled = !uiState.busy && name.isNotBlank() && code.isNotBlank(),
                    ) {
                        Text(if (uiState.busy) "Saving..." else "Save")
                    }
                    TextButton(onClick = ::reset) { Text("Clear") }
                }
            }
        }

        item {
            SectionCard(title = "Institute directory", description = "Every record here stays synced with Supabase.") {
                if (uiState.institutes.isEmpty()) {
                    EmptyState("No institutes available.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        uiState.institutes.forEach { institute ->
                            ClickableInfoRow(
                                headline = "${institute.code} - ${institute.name}",
                                supporting = "${institute.contactNo ?: "No contact"}\nCreated ${formatDate(institute.createdAt)}",
                                trailing = {
                                    StatusChip(label = institute.status, positive = institute.status == "active")
                                },
                                onClick = {
                                    editingId = institute.id
                                    name = institute.name
                                    code = institute.code
                                    address = institute.address.orEmpty()
                                    contactNo = institute.contactNo.orEmpty()
                                    status = institute.status
                                },
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = {
                                    editingId = institute.id
                                    name = institute.name
                                    code = institute.code
                                    address = institute.address.orEmpty()
                                    contactNo = institute.contactNo.orEmpty()
                                    status = institute.status
                                }) { Text("Edit") }
                                TextButton(onClick = { onDelete(institute.id) }) { Text("Delete") }
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StaffScreen(
    uiState: MainUiState,
    modifier: Modifier = Modifier,
    onSave: (String?, StaffDraft) -> Unit,
    onDelete: (String) -> Unit,
) {
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var fullName by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var instituteId by rememberSaveable { mutableStateOf(uiState.institutes.firstOrNull()?.id.orEmpty()) }
    var phone by rememberSaveable { mutableStateOf("") }
    var status by rememberSaveable { mutableStateOf("active") }

    LaunchedEffect(uiState.institutes) {
        if (instituteId.isBlank()) {
            instituteId = uiState.institutes.firstOrNull()?.id.orEmpty()
        }
    }

    fun reset() {
        editingId = null
        fullName = ""
        username = ""
        email = ""
        password = ""
        instituteId = uiState.institutes.firstOrNull()?.id.orEmpty()
        phone = ""
        status = "active"
    }

    LaunchedEffect(uiState.notice) {
        if (uiState.notice == "Staff account saved successfully.") {
            reset()
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            PageHeroCard(
                eyebrow = "Admin workspace",
                title = "Staff operations",
                description = "Create staff accounts, connect them to institutes, and keep access clean without leaving the mobile control center.",
                icon = Icons.Outlined.SupervisorAccount,
                accentColor = AppAccent,
                highlights = listOf(
                    "${uiState.staff.size} staff accounts",
                    "${uiState.institutes.size} institutes linked",
                    "Role-controlled",
                ),
            )
        }

        item {
            SectionCard(
                title = if (editingId == null) "Add staff account" else "Edit staff account",
                description = "Passwords are required for new staff and optional during edits.",
            ) {
                OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("Full name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Username") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(if (editingId == null) "Password" else "New password (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                InstituteSelector(institutes = uiState.institutes, selectedId = instituteId, onSelected = { instituteId = it })
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") }, modifier = Modifier.fillMaxWidth())
                StatusSelector(selected = status, onSelected = { status = it })
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            onSave(editingId, StaffDraft(fullName, username, email, password, instituteId, phone, status))
                        },
                        enabled = !uiState.busy && fullName.isNotBlank() && username.isNotBlank() && email.isNotBlank() && instituteId.isNotBlank() && (editingId != null || password.isNotBlank()),
                    ) {
                        Text(if (uiState.busy) "Saving..." else "Save")
                    }
                    TextButton(onClick = ::reset) { Text("Clear") }
                }
            }
        }

        item {
            SectionCard(title = "Staff directory", description = "Only admins can access this screen.") {
                if (uiState.staff.isEmpty()) {
                    EmptyState("No staff accounts available.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        uiState.staff.forEach { staff ->
                            val instituteName = uiState.institutes.firstOrNull { it.id == staff.instituteId }?.name ?: "Unassigned"
                            ClickableInfoRow(
                                headline = staff.fullName,
                                supporting = "${staff.username} - ${staff.email}\n$instituteName",
                                trailing = { StatusChip(label = staff.status, positive = staff.status == "active") },
                                onClick = {
                                    editingId = staff.id
                                    fullName = staff.fullName
                                    username = staff.username
                                    email = staff.email
                                    password = ""
                                    instituteId = staff.instituteId.orEmpty()
                                    phone = staff.phone.orEmpty()
                                    status = staff.status
                                },
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = {
                                    editingId = staff.id
                                    fullName = staff.fullName
                                    username = staff.username
                                    email = staff.email
                                    password = ""
                                    instituteId = staff.instituteId.orEmpty()
                                    phone = staff.phone.orEmpty()
                                    status = staff.status
                                }) { Text("Edit") }
                                TextButton(onClick = { onDelete(staff.id) }) { Text("Delete") }
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StudentsScreen(
    uiState: MainUiState,
    modifier: Modifier = Modifier,
    onSave: (Student?, StudentDraft) -> Unit,
    onDelete: (String) -> Unit,
    onLoadQr: (String, (QrCodeRecord?) -> Unit) -> Unit,
) {
    val context = LocalContext.current
    val exportHelper = remember(context) { ExportHelper(context) }
    var editingStudent by remember { mutableStateOf<Student?>(null) }
    var qrDialog by remember { mutableStateOf<QrCodeRecord?>(null) }
    var qrStudentName by rememberSaveable { mutableStateOf("") }
    var fullName by rememberSaveable { mutableStateOf("") }
    var studentCode by rememberSaveable { mutableStateOf("") }
    var alYear by rememberSaveable { mutableStateOf("2028") }
    var instituteId by rememberSaveable { mutableStateOf(uiState.profile?.instituteId ?: uiState.institutes.firstOrNull()?.id.orEmpty()) }
    var monthlyFee by rememberSaveable { mutableStateOf("0") }
    var whatsapp by rememberSaveable { mutableStateOf("") }
    var joinedDate by rememberSaveable { mutableStateOf(java.time.LocalDate.now().toString()) }
    var status by rememberSaveable { mutableStateOf("active") }
    var instituteFilter by rememberSaveable { mutableStateOf("all") }
    var yearFilter by rememberSaveable { mutableStateOf("all") }
    var selectedStudentId by rememberSaveable { mutableStateOf<String?>(null) }

    fun editableMoney(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.toLong().toString()
        } else {
            value.toString()
        }
    }

    fun reset() {
        editingStudent = null
        fullName = ""
        studentCode = ""
        alYear = "2028"
        instituteId = uiState.profile?.instituteId ?: uiState.institutes.firstOrNull()?.id.orEmpty()
        monthlyFee = "0"
        whatsapp = ""
        joinedDate = java.time.LocalDate.now().toString()
        status = "active"
    }

    LaunchedEffect(uiState.notice) {
        if (uiState.notice == "Student added successfully." || uiState.notice == "Student updated successfully.") {
            reset()
        }
    }

    val visibleStudents = remember(uiState.students, instituteFilter, yearFilter) {
        uiState.students.filter { student ->
            (instituteFilter == "all" || student.instituteId == instituteFilter) &&
                (yearFilter == "all" || student.alYear.toString() == yearFilter)
        }
    }
    val selectedStudent = uiState.students.firstOrNull { it.id == selectedStudentId }

    QrPreviewDialog(qrCode = qrDialog, studentName = qrStudentName, onDismiss = { qrDialog = null })
    if (selectedStudent != null) {
        StudentDetailDialog(
            student = selectedStudent,
            instituteName = uiState.institutes.firstOrNull { it.id == selectedStudent.instituteId }?.name ?: "-",
            attendance = uiState.attendance.filter { it.studentId == selectedStudent.id },
            payments = uiState.payments.filter { it.studentId == selectedStudent.id },
            onDismiss = { selectedStudentId = null },
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            PageHeroCard(
                eyebrow = "Student hub",
                title = "Students and QR identity",
                description = "Register students, manage WhatsApp-ready QR cards, and jump into details without breaking the flow.",
                icon = Icons.Outlined.PeopleAlt,
                accentColor = AppPrimary,
                highlights = listOf(
                    "${uiState.students.size} students",
                    "${visibleStudents.size} in view",
                    "Share QR as image",
                ),
            )
        }

        item {
            SectionCard(
                title = if (editingStudent == null) "Add student" else "Edit student",
                description = "Each saved student keeps one unique QR and a default monthly class fee ready for payment tracking.",
            ) {
                OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("Student name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = studentCode, onValueChange = { studentCode = it }, label = { Text("Student code") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = alYear, onValueChange = { alYear = it.filter(Char::isDigit) }, label = { Text("A/L year") }, modifier = Modifier.fillMaxWidth())
                InstituteSelector(
                    institutes = uiState.institutes,
                    selectedId = instituteId,
                    enabled = uiState.profile?.role == Role.Admin,
                    onSelected = { instituteId = it },
                )
                OutlinedTextField(
                    value = monthlyFee,
                    onValueChange = { input ->
                        monthlyFee = buildString {
                            var dotUsed = false
                            input.forEach { char ->
                                when {
                                    char.isDigit() -> append(char)
                                    char == '.' && !dotUsed -> {
                                        append(char)
                                        dotUsed = true
                                    }
                                }
                            }
                        }
                    },
                    label = { Text("Monthly class fee (LKR)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(value = whatsapp, onValueChange = { whatsapp = it }, label = { Text("WhatsApp number") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = joinedDate, onValueChange = { joinedDate = it }, label = { Text("Joined date (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
                StatusSelector(selected = status, onSelected = { status = it })
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            onSave(
                                editingStudent,
                                StudentDraft(
                                    studentCode = studentCode,
                                    fullName = fullName,
                                    alYear = alYear.toIntOrNull() ?: 2028,
                                    instituteId = instituteId,
                                    monthlyFee = monthlyFee.toDoubleOrNull() ?: 0.0,
                                    whatsappNumber = whatsapp,
                                    joinedDate = joinedDate,
                                    status = status,
                                ),
                            )
                        },
                        enabled = !uiState.busy && fullName.isNotBlank() && instituteId.isNotBlank() && whatsapp.isNotBlank(),
                    ) {
                        Text(if (uiState.busy) "Saving..." else "Save")
                    }
                    TextButton(onClick = ::reset) { Text("Clear") }
                }
            }
        }

        item {
            SectionCard(title = "Student roster", description = "Admins can filter by institute and year, then edit, open, or share QR images.") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (uiState.profile?.role == Role.Admin) {
                        FilterSelector(
                            label = "Institute",
                            selected = instituteFilter,
                            options = listOf("all" to "All institutes") + uiState.institutes.map { it.id to it.name },
                            onSelected = { instituteFilter = it },
                        )
                    }
                    FilterSelector(
                        label = "Year",
                        selected = yearFilter,
                        options = listOf("all" to "All years") + uiState.students.map { it.alYear.toString() to it.alYear.toString() }.distinct(),
                        onSelected = { yearFilter = it },
                    )
                }

                if (visibleStudents.isEmpty()) {
                    EmptyState("No students match the current filters.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        visibleStudents.forEach { student ->
                            val instituteName = uiState.institutes.firstOrNull { it.id == student.instituteId }?.name ?: "-"
                            ClickableInfoRow(
                                headline = student.fullName,
                                supporting = "${student.studentCode ?: "-"} - $instituteName - ${student.alYear}\n${student.whatsappNumber}  |  ${formatCurrency(student.monthlyFee)} / month",
                                trailing = { StatusChip(label = student.status, positive = student.status == "active") },
                                onClick = { selectedStudentId = student.id },
                            )
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                TextButton(onClick = {
                                    editingStudent = student
                                    fullName = student.fullName
                                    studentCode = student.studentCode.orEmpty()
                                    alYear = student.alYear.toString()
                                    instituteId = student.instituteId
                                    monthlyFee = editableMoney(student.monthlyFee)
                                    whatsapp = student.whatsappNumber
                                    joinedDate = student.joinedDate
                                    status = student.status
                                }) { Text("Edit") }
                                TextButton(onClick = { onDelete(student.id) }) { Text("Delete") }
                                TextButton(onClick = {
                                    val cached = uiState.qrCodes.firstOrNull { it.studentId == student.id }
                                    qrStudentName = student.fullName
                                    if (cached != null) {
                                        qrDialog = cached
                                    } else {
                                        onLoadQr(student.id) { qrDialog = it }
                                    }
                                }) { Text("Open QR") }
                                TextButton(onClick = {
                                    val cached = uiState.qrCodes.firstOrNull { it.studentId == student.id }
                                    if (cached != null) {
                                        exportHelper.shareStudentQrImage(cached, student.fullName, student.whatsappNumber)
                                    } else {
                                        onLoadQr(student.id) { loadedQr ->
                                            if (loadedQr != null) {
                                                exportHelper.shareStudentQrImage(loadedQr, student.fullName, student.whatsappNumber)
                                            }
                                        }
                                    }
                                }) { Text("Share QR") }
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StudentDetailDialog(
    student: Student,
    instituteName: String,
    attendance: List<AttendanceRecord>,
    payments: List<PaymentRecord>,
    onDismiss: () -> Unit,
) {
    val monthOptions = remember(student.id, attendance, payments) {
        buildStudentMonthOptions(student, attendance, payments)
    }
    val defaultMonthKey = monthOptions.firstOrNull { it.key == currentMonthKey() }?.key
        ?: monthOptions.firstOrNull()?.key
        ?: currentMonthKey()
    var selectedMonthKey by remember(student.id) { mutableStateOf(defaultMonthKey) }

    LaunchedEffect(student.id, defaultMonthKey) {
        selectedMonthKey = defaultMonthKey
    }

    val monthlyAttendance = remember(attendance, selectedMonthKey) {
        attendanceForMonth(attendance, selectedMonthKey)
    }
    val monthlyPayments = remember(payments, selectedMonthKey) {
        paymentsForMonth(payments, selectedMonthKey)
    }
    val attendanceTrend = remember(attendance) { buildStudentAttendanceTrend(attendance) }
    val paymentTrend = remember(payments) { buildStudentPaymentTrend(payments) }
    val totalAttendance = remember(attendance) { countAttendedClasses(attendance) }
    val monthlyAttendanceCount = remember(monthlyAttendance) { countAttendedClasses(monthlyAttendance) }
    val lateCount = remember(monthlyAttendance) { monthlyAttendance.count { it.status.lowercase() == "late" } }
    val paidMonthsCount = remember(payments) { payments.count { it.paid } }
    val selectedMonthPaid = remember(monthlyPayments) { monthlyPayments.any { it.paid } }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .heightIn(max = 820.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            "Student insight",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            student.fullName,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                        )
                        Text(
                            "Open the student profile, attendance dates, and payment history without leaving the student list.",
                            color = AppMutedText,
                        )
                    }
                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    StudentMetricCard(label = "Total attended", value = totalAttendance.toString(), hint = "All recorded classes")
                    StudentMetricCard(label = formatMonthKeyLabel(selectedMonthKey), value = monthlyAttendanceCount.toString(), hint = "$lateCount late marks")
                    StudentMetricCard(label = "Paid months", value = paidMonthsCount.toString(), hint = "Completed payment records")
                    StudentMetricCard(label = "Selected month", value = if (selectedMonthPaid) "Paid" else "Pending", hint = "Payment status")
                }

                SectionCard(
                    title = "Overview",
                    description = "Core student information and quick access to the current QR link.",
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        StudentInfoCard(label = "Student code", value = student.studentCode ?: "-")
                        StudentInfoCard(label = "Institute", value = instituteName)
                        StudentInfoCard(label = "A/L year", value = student.alYear.toString())
                        StudentInfoCard(label = "Monthly fee", value = formatCurrency(student.monthlyFee))
                        StudentInfoCard(label = "WhatsApp", value = student.whatsappNumber)
                        StudentInfoCard(label = "Joined date", value = formatDate(student.joinedDate))
                        StudentInfoCard(label = "QR status", value = if (student.qrLink.isNullOrBlank()) "Not ready" else "Ready to preview/share")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusChip(label = student.status, positive = student.status == "active")
                    }
                }

                SectionCard(
                    title = "Monthly attendance",
                    description = "Attendance dates for the selected month.",
                ) {
                    FilterSelector(
                        label = "Month",
                        selected = selectedMonthKey,
                        options = monthOptions.map { it.key to it.label },
                        onSelected = { selectedMonthKey = it },
                    )

                    if (monthlyAttendance.isEmpty()) {
                        EmptyState("No attendance was recorded for this month yet.")
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            monthlyAttendance.forEach { record ->
                                AttendanceDateCard(
                                    dateLabel = formatShortAttendanceDate(record.attendanceDate),
                                    status = record.status,
                                )
                            }
                        }
                    }
                }

                TrendChart(
                    title = "Attendance graph",
                    subtitle = "Classes attended across the latest six months.",
                    points = attendanceTrend,
                )

                TrendChart(
                    title = "Payment graph",
                    subtitle = "A bar value of 1 means the month is already paid.",
                    points = paymentTrend,
                    barMode = true,
                )

                SectionCard(
                    title = "Payment details",
                    description = "Payment entries recorded for the selected month.",
                ) {
                    if (monthlyPayments.isEmpty()) {
                        EmptyState("No payment record was added for this month.")
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            monthlyPayments.forEach { payment ->
                                PaymentDetailCard(payment = payment)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StudentMetricCard(label: String, value: String, hint: String) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = AppSurfaceMuted),
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .heightIn(min = 110.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(label, color = AppMutedText, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            Text(hint, color = AppMutedText, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun StudentInfoCard(label: String, value: String) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = AppSurfaceMuted),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(label, color = AppMutedText, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun AttendanceDateCard(dateLabel: String, status: String) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = AppSurfaceMuted,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(dateLabel, fontWeight = FontWeight.SemiBold)
            StatusChip(label = status, positive = status != "absent")
        }
    }
}

@Composable
private fun PaymentDetailCard(payment: PaymentRecord) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = AppSurfaceMuted),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    if (payment.paid) "Paid" else "Pending",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "${payment.paymentMonth}/${payment.paymentYear}",
                    color = AppMutedText,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Paid date", color = AppMutedText, style = MaterialTheme.typography.labelMedium)
                Text(formatDate(payment.paidDate), fontWeight = FontWeight.SemiBold)
                Text(formatCurrency(payment.amount), color = AppMutedText, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun InstituteSelector(
    institutes: List<Institute>,
    selectedId: String,
    enabled: Boolean = true,
    onSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = institutes.firstOrNull { it.id == selectedId }?.name ?: "Select institute"

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text("Institute") },
            modifier = Modifier.fillMaxWidth(),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(enabled = enabled) { expanded = true },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            institutes.forEach { institute ->
                DropdownMenuItem(
                    text = { Text(institute.name) },
                    onClick = {
                        onSelected(institute.id)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
fun StatusSelector(selected: String, onSelected: (String) -> Unit) {
    FilterSelector(
        label = "Status",
        selected = selected,
        options = listOf("active" to "Active", "inactive" to "Inactive"),
        onSelected = onSelected,
    )
}

@Composable
fun FilterSelector(
    label: String,
    selected: String,
    options: List<Pair<String, String>>,
    onSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val safeOptions = if (options.isEmpty()) listOf("" to "No options") else options
    val selectedLabel = safeOptions.firstOrNull { it.first == selected }?.second ?: safeOptions.first().second

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { expanded = true },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            safeOptions.forEach { (value, title) ->
                DropdownMenuItem(
                    text = { Text(title) },
                    onClick = {
                        onSelected(value)
                        expanded = false
                    },
                )
            }
        }
    }
}
