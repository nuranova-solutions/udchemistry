package com.udchemistry.mobile.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.SupervisorAccount
import androidx.compose.ui.graphics.vector.ImageVector
import com.udchemistry.mobile.model.Role

enum class AppDestination(
    val title: String,
    val icon: ImageVector,
    val roles: Set<Role>,
) {
    Dashboard("Dashboard", Icons.Outlined.Dashboard, setOf(Role.Admin, Role.Staff)),
    Institutes("Institutes", Icons.Outlined.School, setOf(Role.Admin)),
    Staff("Staff", Icons.Outlined.SupervisorAccount, setOf(Role.Admin)),
    Classes("Classes", Icons.AutoMirrored.Outlined.MenuBook, setOf(Role.Admin, Role.Staff)),
    Students("Students", Icons.Outlined.People, setOf(Role.Admin, Role.Staff)),
    Attendance("Attendance", Icons.Outlined.Checklist, setOf(Role.Admin, Role.Staff)),
    Payments("Payments", Icons.Outlined.AttachMoney, setOf(Role.Admin, Role.Staff)),
    Reports("Reports", Icons.Outlined.Assessment, setOf(Role.Admin, Role.Staff)),
    Help("Help", Icons.AutoMirrored.Outlined.HelpOutline, setOf(Role.Admin, Role.Staff)),
    Profile("Profile", Icons.Outlined.AccountCircle, setOf(Role.Admin, Role.Staff)),
}
