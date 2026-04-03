package com.udchemistry.mobile.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.udchemistry.mobile.model.ClassDraft
import com.udchemistry.mobile.model.ClassRecord
import com.udchemistry.mobile.model.Role
import com.udchemistry.mobile.ui.MainUiState
import com.udchemistry.mobile.ui.components.ClickableInfoRow
import com.udchemistry.mobile.ui.components.EmptyState
import com.udchemistry.mobile.ui.components.PageHeroCard
import com.udchemistry.mobile.ui.components.SectionCard
import com.udchemistry.mobile.ui.components.StatusChip
import com.udchemistry.mobile.ui.theme.AppAccent
import com.udchemistry.mobile.ui.theme.AppDivider
import com.udchemistry.mobile.ui.theme.AppMutedText
import com.udchemistry.mobile.ui.theme.AppPrimary
import com.udchemistry.mobile.util.formatCurrency
import java.time.LocalDate

private val classTypeOptions = listOf(
    "general" to "General",
    "extra" to "Extra",
)

private val weekdayOptions = listOf(
    "monday" to "Monday",
    "tuesday" to "Tuesday",
    "wednesday" to "Wednesday",
    "thursday" to "Thursday",
    "friday" to "Friday",
    "saturday" to "Saturday",
    "sunday" to "Sunday",
)

private val weekOfMonthOptions = listOf(
    "1" to "First week",
    "2" to "Second week",
    "3" to "Third week",
    "4" to "Fourth week",
    "5" to "Fifth week",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ClassesScreen(
    uiState: MainUiState,
    modifier: Modifier = Modifier,
    onSave: (ClassRecord?, ClassDraft) -> Unit,
    onDelete: (String) -> Unit,
) {
    var editingClass by remember { mutableStateOf<ClassRecord?>(null) }
    val defaultInstituteId = remember(uiState.profile?.role, uiState.profile?.instituteId, uiState.institutes) {
        when {
            uiState.profile?.role == Role.Admin -> uiState.institutes.firstOrNull()?.id.orEmpty()
            else -> uiState.profile?.instituteId.orEmpty()
        }
    }
    var name by rememberSaveable { mutableStateOf("") }
    var instituteId by rememberSaveable { mutableStateOf(defaultInstituteId) }
    var alYear by rememberSaveable { mutableStateOf(LocalDate.now().year.toString()) }
    var monthlyFee by rememberSaveable { mutableStateOf("") }
    var classType by rememberSaveable { mutableStateOf("general") }
    var weekday by rememberSaveable { mutableStateOf("monday") }
    var startTime by rememberSaveable { mutableStateOf("09:00") }
    var endTime by rememberSaveable { mutableStateOf("11:00") }
    var weekOfMonth by rememberSaveable { mutableStateOf("1") }
    var activeFrom by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var activeUntil by rememberSaveable { mutableStateOf("") }
    var status by rememberSaveable { mutableStateOf("active") }
    var notes by rememberSaveable { mutableStateOf("") }
    var instituteFilter by rememberSaveable { mutableStateOf("all") }
    var yearFilter by rememberSaveable { mutableStateOf("all") }
    var typeFilter by rememberSaveable { mutableStateOf("all") }
    var weekdayFilter by rememberSaveable { mutableStateOf("all") }

    fun resetForm() {
        editingClass = null
        name = ""
        instituteId = defaultInstituteId
        alYear = LocalDate.now().year.toString()
        monthlyFee = ""
        classType = "general"
        weekday = "monday"
        startTime = "09:00"
        endTime = "11:00"
        weekOfMonth = "1"
        activeFrom = LocalDate.now().toString()
        activeUntil = ""
        status = "active"
        notes = ""
    }

    LaunchedEffect(defaultInstituteId, uiState.profile?.role) {
        if (uiState.profile?.role == Role.Staff) {
            instituteId = defaultInstituteId
        } else if (instituteId.isBlank()) {
            instituteId = defaultInstituteId
        }
    }

    LaunchedEffect(uiState.notice) {
        if (uiState.notice == "Class saved successfully." || uiState.notice == "Class updated successfully.") {
            resetForm()
        }
    }

    val visibleClasses = remember(uiState.classes, instituteFilter, yearFilter, typeFilter, weekdayFilter) {
        uiState.classes.filter { classItem ->
            (instituteFilter == "all" || classItem.instituteId == instituteFilter) &&
                (yearFilter == "all" || classItem.alYear.toString() == yearFilter) &&
                (typeFilter == "all" || classItem.classType == typeFilter) &&
                (weekdayFilter == "all" || classItem.weekday == weekdayFilter)
        }
    }
    val activeClasses = remember(uiState.classes) { uiState.classes.count { it.status == "active" } }
    val generalClasses = remember(uiState.classes) { uiState.classes.count { it.classType == "general" } }
    val extraClasses = remember(uiState.classes) { uiState.classes.count { it.classType == "extra" } }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            PageHeroCard(
                eyebrow = "Class control",
                title = "Classes and recurring schedule",
                description = "Manage the normal weekly class plan and one-off extra class patterns from Android without breaking the premium scan-first flow.",
                icon = Icons.AutoMirrored.Outlined.MenuBook,
                accentColor = AppPrimary,
                highlights = listOf(
                    "${uiState.classes.size} total classes",
                    "$activeClasses active",
                    "$generalClasses normal / $extraClasses extra",
                ),
            )
        }

        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ClassSummaryCard(label = "Active classes", value = activeClasses.toString(), hint = "Live schedules", accent = AppPrimary)
                ClassSummaryCard(label = "General classes", value = generalClasses.toString(), hint = "Weekly recurring", accent = AppAccent)
                ClassSummaryCard(label = "Extra classes", value = extraClasses.toString(), hint = "Patterned add-ons", accent = AppPrimary)
            }
        }

        item {
            SectionCard(
                title = if (editingClass == null) "Add class" else "Edit class",
                description = "Start with the normal weekly class, then add extra classes only when you need them.",
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Class name") },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (uiState.profile?.role == Role.Admin) {
                    InstituteSelector(
                        institutes = uiState.institutes,
                        selectedId = instituteId,
                        onSelected = { instituteId = it },
                    )
                } else {
                    InstituteSelector(
                        institutes = uiState.institutes,
                        selectedId = instituteId,
                        enabled = false,
                        onSelected = { instituteId = it },
                    )
                }
                OutlinedTextField(
                    value = alYear,
                    onValueChange = { alYear = it.filter(Char::isDigit).take(4) },
                    label = { Text("A/L year") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = monthlyFee,
                    onValueChange = { monthlyFee = it.filter { char -> char.isDigit() || char == '.' } },
                    label = { Text("Monthly fee (LKR)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                FilterSelector(
                    label = "Class type",
                    selected = classType,
                    options = classTypeOptions,
                    onSelected = { classType = it },
                )
                FilterSelector(
                    label = "Weekday",
                    selected = weekday,
                    options = weekdayOptions,
                    onSelected = { weekday = it },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = sanitizeTimeInput(it) },
                        label = { Text("Start time") },
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = sanitizeTimeInput(it) },
                        label = { Text("End time") },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (classType == "extra") {
                    FilterSelector(
                        label = "Week of month",
                        selected = weekOfMonth,
                        options = weekOfMonthOptions,
                        onSelected = { weekOfMonth = it },
                    )
                }
                OutlinedTextField(
                    value = activeFrom,
                    onValueChange = { activeFrom = it },
                    label = { Text("Active from (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = activeUntil,
                    onValueChange = { activeUntil = it },
                    label = { Text("Active until (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                StatusSelector(
                    selected = status,
                    onSelected = { status = it },
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp),
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = {
                            onSave(
                                editingClass,
                                ClassDraft(
                                    name = name,
                                    instituteId = instituteId,
                                    alYear = alYear.toIntOrNull() ?: LocalDate.now().year,
                                    monthlyFee = monthlyFee.toDoubleOrNull() ?: 0.0,
                                    classType = classType,
                                    weekday = weekday,
                                    startTime = startTime,
                                    endTime = endTime,
                                    weekOfMonth = if (classType == "extra") weekOfMonth.toIntOrNull() else null,
                                    activeFrom = activeFrom,
                                    activeUntil = activeUntil.ifBlank { null },
                                    status = status,
                                    notes = notes,
                                ),
                            )
                        },
                        enabled = !uiState.busy && name.isNotBlank() && instituteId.isNotBlank(),
                    ) {
                        Text(if (uiState.busy) "Saving..." else "Save")
                    }
                    TextButton(onClick = ::resetForm) {
                        Text("Clear")
                    }
                }
            }
        }

        item {
            SectionCard(
                title = "Class roster",
                description = "Filter schedules by institute, year, type, or weekday, then edit the class plan in place.",
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
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
                        options = listOf("all" to "All years") + uiState.classes.map { it.alYear.toString() to it.alYear.toString() }.distinct(),
                        onSelected = { yearFilter = it },
                    )
                    FilterSelector(
                        label = "Type",
                        selected = typeFilter,
                        options = listOf("all" to "All types") + classTypeOptions,
                        onSelected = { typeFilter = it },
                    )
                    FilterSelector(
                        label = "Weekday",
                        selected = weekdayFilter,
                        options = listOf("all" to "All days") + weekdayOptions,
                        onSelected = { weekdayFilter = it },
                    )
                }

                if (visibleClasses.isEmpty()) {
                    EmptyState("No classes match the current filters yet.")
                } else {
                    visibleClasses.forEach { classItem ->
                        val instituteName = uiState.institutes.firstOrNull { it.id == classItem.instituteId }?.name ?: "-"
                        ClickableInfoRow(
                            headline = classItem.name,
                            supporting = buildClassSupportingText(classItem, instituteName),
                            trailing = {
                                StatusChip(
                                    label = classItem.status,
                                    positive = classItem.status == "active",
                                )
                            },
                            onClick = {
                                editingClass = classItem
                                name = classItem.name
                                instituteId = classItem.instituteId
                                alYear = classItem.alYear.toString()
                                monthlyFee = editableMoney(classItem.monthlyFee)
                                classType = classItem.classType
                                weekday = classItem.weekday
                                startTime = displayTime(classItem.startTime)
                                endTime = displayTime(classItem.endTime)
                                weekOfMonth = classItem.weekOfMonth?.toString() ?: "1"
                                activeFrom = classItem.activeFrom
                                activeUntil = classItem.activeUntil.orEmpty()
                                status = classItem.status
                                notes = classItem.notes.orEmpty()
                            },
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            TextButton(
                                onClick = {
                                    editingClass = classItem
                                    name = classItem.name
                                    instituteId = classItem.instituteId
                                    alYear = classItem.alYear.toString()
                                    monthlyFee = editableMoney(classItem.monthlyFee)
                                    classType = classItem.classType
                                    weekday = classItem.weekday
                                    startTime = displayTime(classItem.startTime)
                                    endTime = displayTime(classItem.endTime)
                                    weekOfMonth = classItem.weekOfMonth?.toString() ?: "1"
                                    activeFrom = classItem.activeFrom
                                    activeUntil = classItem.activeUntil.orEmpty()
                                    status = classItem.status
                                    notes = classItem.notes.orEmpty()
                                },
                            ) {
                                Text("Edit")
                            }
                            TextButton(onClick = { onDelete(classItem.id) }) {
                                Text("Delete")
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun ClassSummaryCard(
    label: String,
    value: String,
    hint: String,
    accent: androidx.compose.ui.graphics.Color,
) {
    Surface(
        modifier = Modifier.widthIn(min = 168.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        border = BorderStroke(0.7.dp, AppDivider),
    ) {
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            androidx.compose.foundation.layout.Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(label, color = AppMutedText, style = MaterialTheme.typography.labelLarge)
                Text(value, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold, color = accent)
                Text(hint, color = AppMutedText, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun buildClassSupportingText(classItem: ClassRecord, instituteName: String): String {
    val typeLabel = classTypeOptions.firstOrNull { it.first == classItem.classType }?.second ?: classItem.classType
    val weekdayLabel = weekdayOptions.firstOrNull { it.first == classItem.weekday }?.second ?: classItem.weekday
    val scheduleLabel = if (classItem.classType == "extra" && classItem.weekOfMonth != null) {
        "${weekOfMonthLabel(classItem.weekOfMonth)} $weekdayLabel"
    } else {
        "Every $weekdayLabel"
    }
    val untilLabel = classItem.activeUntil?.let { " until $it" }.orEmpty()
    val noteLine = classItem.notes?.takeIf { it.isNotBlank() }?.let { "\n$it" }.orEmpty()
    return "$instituteName  |  ${classItem.alYear}  |  $typeLabel\n$scheduleLabel  |  ${displayTime(classItem.startTime)} - ${displayTime(classItem.endTime)}  |  ${formatCurrency(classItem.monthlyFee)} / month\nFrom ${classItem.activeFrom}$untilLabel$noteLine"
}

private fun editableMoney(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toLong().toString()
    } else {
        value.toString()
    }
}

private fun sanitizeTimeInput(value: String): String {
    val filtered = value.filter { it.isDigit() || it == ':' }
    return if (filtered.length <= 5) filtered else filtered.take(5)
}

private fun displayTime(value: String): String {
    return when {
        value.length >= 5 -> value.take(5)
        else -> value
    }
}

private fun weekOfMonthLabel(weekOfMonth: Int): String = when (weekOfMonth) {
    1 -> "First week"
    2 -> "Second week"
    3 -> "Third week"
    4 -> "Fourth week"
    5 -> "Fifth week"
    else -> "Week $weekOfMonth"
}
