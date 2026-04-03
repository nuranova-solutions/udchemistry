package com.udchemistry.mobile.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.udchemistry.mobile.ui.MainUiState
import com.udchemistry.mobile.ui.components.MetricCard
import com.udchemistry.mobile.ui.components.PageHeroCard
import com.udchemistry.mobile.ui.components.SectionCard
import com.udchemistry.mobile.ui.theme.AppAccent
import com.udchemistry.mobile.ui.theme.AppMutedText
import com.udchemistry.mobile.ui.theme.AppPrimary
import com.udchemistry.mobile.ui.theme.ThemeMode
import com.udchemistry.mobile.util.ExportHelper

@Composable
fun ReportsScreen(
    uiState: MainUiState,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val exportHelper = remember(context) { ExportHelper(context) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            PageHeroCard(
                eyebrow = "Summary center",
                title = "Reports and exports",
                description = "Turn live students, payments, and attendance data into shareable outputs without leaving the mobile workspace.",
                icon = Icons.Outlined.Assessment,
                accentColor = AppPrimary,
                highlights = listOf(
                    "${uiState.students.size} students",
                    "${uiState.payments.size} payments",
                    "CSV + PDF ready",
                ),
            )
        }

        item {
            SectionCard(
                title = "Reports and exports",
                description = "Share the current roster, payments, or a quick PDF summary straight from Android.",
            ) {
                Button(
                    onClick = { exportHelper.shareStudentsCsv(uiState.students, uiState.institutes) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.students.isNotEmpty(),
                ) {
                    Text("Export students CSV")
                }
                Button(
                    onClick = { exportHelper.sharePaymentsCsv(uiState.payments, uiState.students) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.payments.isNotEmpty(),
                ) {
                    Text("Export payments CSV")
                }
                Button(
                    onClick = { exportHelper.shareSummaryPdf(uiState.dashboard, uiState.attendance, uiState.payments) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Export summary PDF")
                }
            }
        }

        item {
            SectionCard(
                title = "Live report snapshot",
                description = "These numbers mirror the current mobile dataset from Supabase.",
            ) {
                uiState.dashboard.metrics.forEach { metric ->
                    MetricCard(metric = metric, modifier = Modifier.fillMaxWidth())
                }
                Text(
                    "Institutes: ${uiState.institutes.size} | Staff: ${uiState.staff.size} | Students: ${uiState.students.size}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )
            }
        }
    }
}

@Composable
fun ProfileScreen(
    uiState: MainUiState,
    modifier: Modifier = Modifier,
    themeMode: ThemeMode,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onSave: (String, String?, String) -> Unit,
) {
    val profile = uiState.profile
    var fullName by rememberSaveable(profile?.id) { mutableStateOf(profile?.fullName.orEmpty()) }
    var phone by rememberSaveable(profile?.id) { mutableStateOf(profile?.phone.orEmpty()) }
    var password by rememberSaveable(profile?.id) { mutableStateOf("") }

    LaunchedEffect(profile?.id, profile?.fullName, profile?.phone) {
        fullName = profile?.fullName.orEmpty()
        phone = profile?.phone.orEmpty()
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            PageHeroCard(
                eyebrow = "Account center",
                title = "Profile and access",
                description = "Update your account details, keep contact info accurate, and control password changes from a cleaner settings experience.",
                icon = Icons.Outlined.AccountCircle,
                accentColor = AppAccent,
                highlights = listOf(
                    profile?.role?.name ?: "Staff",
                    profile?.username.orEmpty(),
                    "Secure mobile session",
                ),
            )
        }

        item {
            SectionCard(
                title = "Appearance",
                description = "Dark mode keeps the premium branded look you approved. Light mode keeps the same UX structure with brighter surfaces, and System follows the device setting.",
            ) {
                ThemeModeOption(
                    title = "System",
                    description = "Match the phone theme automatically.",
                    selected = themeMode == ThemeMode.System,
                    onClick = { onThemeModeSelected(ThemeMode.System) },
                )
                ThemeModeOption(
                    title = "Dark",
                    description = "Keep the current premium glass interface.",
                    selected = themeMode == ThemeMode.Dark,
                    onClick = { onThemeModeSelected(ThemeMode.Dark) },
                )
                ThemeModeOption(
                    title = "Light",
                    description = "Use the same layout and actions with brighter contrast.",
                    selected = themeMode == ThemeMode.Light,
                    onClick = { onThemeModeSelected(ThemeMode.Light) },
                )
            }
        }

        item {
            SectionCard(
                title = "Profile",
                description = "Update your personal details and password. Mobile login stays active for 24 hours after sign-in.",
            ) {
                Text(
                    profile?.role?.name ?: "",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full name") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = profile?.username.orEmpty(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Username") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = profile?.email.orEmpty(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("New password") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onSave(fullName, phone.ifBlank { null }, password) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.busy && fullName.isNotBlank(),
                    ) {
                        Text(if (uiState.busy) "Saving..." else "Save profile")
                    }
                    TextButton(
                        onClick = {
                            fullName = profile?.fullName.orEmpty()
                            phone = profile?.phone.orEmpty()
                            password = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Reset")
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeModeOption(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        color = if (selected) AppPrimary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
        border = androidx.compose.foundation.BorderStroke(
            0.8.dp,
            if (selected) AppPrimary.copy(alpha = 0.44f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (selected) {
                    Text(
                        text = "Active",
                        modifier = Modifier.align(androidx.compose.ui.Alignment.TopEnd),
                        style = MaterialTheme.typography.labelMedium,
                        color = AppPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = AppMutedText,
            )
        }
    }
}

@Composable
fun HelpScreen(
    uiState: MainUiState,
    modifier: Modifier = Modifier,
) {
    val roleLabel = uiState.profile?.role?.name ?: "Staff"

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            PageHeroCard(
                eyebrow = "Usage guide",
                title = "Help and how to use the app",
                description = "Use this page to learn the basic flow for sign-in, dashboard use, QR scanning, and monthly records.",
                icon = Icons.AutoMirrored.Outlined.HelpOutline,
                accentColor = AppPrimary,
                highlights = listOf(
                    roleLabel,
                    "QR attendance",
                    "Payments + students",
                ),
            )
        }

        item {
            SectionCard(
                title = "Start here",
                description = "The quickest way to begin using the app.",
            ) {
                HelpLine("1. Sign in with your username and password.")
                HelpLine("2. Open the dashboard to see attendance, unpaid students, and shortcuts.")
                HelpLine("3. Use the Scan button to scan a student QR code for attendance.")
            }
        }

        item {
            SectionCard(
                title = "Main pages",
                description = "What each important page is used for.",
            ) {
                HelpLine("Dashboard: View the main activity for your workspace.")
                HelpLine("Students: Register students, open QR codes, and share QR images.")
                HelpLine("Attendance: Scan QR codes or correct attendance records manually.")
                HelpLine("Payments: Add, edit, and review monthly payment records.")
                HelpLine("Summary: Export CSV and PDF reports when needed.")
            }
        }

        item {
            SectionCard(
                title = "Good workflow",
                description = "A simple daily routine for staff and admins.",
            ) {
                HelpLine("Check the dashboard first.")
                HelpLine("Scan students as they arrive.")
                HelpLine("Review unpaid students and payment updates.")
                HelpLine("Open Settings when you need to update your profile.")
            }
        }
    }
}

@Composable
private fun HelpLine(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = AppMutedText,
    )
}
