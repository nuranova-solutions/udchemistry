package com.udchemistry.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.PersonAddAlt1
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.udchemistry.mobile.model.AttendanceDraft
import com.udchemistry.mobile.model.ClassDraft
import com.udchemistry.mobile.model.ClassRecord
import com.udchemistry.mobile.model.InstituteDraft
import com.udchemistry.mobile.model.PaymentDraft
import com.udchemistry.mobile.model.Profile
import com.udchemistry.mobile.model.QrCodeRecord
import com.udchemistry.mobile.model.Role
import com.udchemistry.mobile.model.StaffDraft
import com.udchemistry.mobile.model.Student
import com.udchemistry.mobile.model.StudentDraft
import com.udchemistry.mobile.ui.components.AdaptiveContentFrame
import com.udchemistry.mobile.ui.components.AppGradientBackground
import com.udchemistry.mobile.ui.components.GlassPanel
import com.udchemistry.mobile.ui.navigation.AppDestination
import com.udchemistry.mobile.ui.screens.AttendanceScreen
import com.udchemistry.mobile.ui.screens.ClassesScreen
import com.udchemistry.mobile.ui.screens.DashboardScreen
import com.udchemistry.mobile.ui.screens.HelpScreen
import com.udchemistry.mobile.ui.screens.InstitutesScreen
import com.udchemistry.mobile.ui.screens.PaymentsScreen
import com.udchemistry.mobile.ui.screens.ProfileScreen
import com.udchemistry.mobile.ui.screens.ReportsScreen
import com.udchemistry.mobile.ui.screens.StaffScreen
import com.udchemistry.mobile.ui.screens.StudentsScreen
import com.udchemistry.mobile.ui.theme.AppAccent
import com.udchemistry.mobile.ui.theme.AppDanger
import com.udchemistry.mobile.ui.theme.AppGlassStrong
import com.udchemistry.mobile.ui.theme.AppMutedText
import com.udchemistry.mobile.ui.theme.AppOrangeGlow
import com.udchemistry.mobile.ui.theme.AppPrimary
import com.udchemistry.mobile.ui.theme.AppPurpleGlow
import com.udchemistry.mobile.ui.theme.ThemeMode
import com.udchemistry.mobile.ui.theme.UDChemistryTheme
import com.udchemistry.mobile.ui.theme.shouldUseDarkTheme
import java.time.LocalDate

@Composable
fun ChemistryMobileApp(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.notice, uiState.error) {
        val message = uiState.error ?: uiState.notice ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearFeedback()
    }

    AppGradientBackground {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
            when {
                uiState.bootstrapping -> SplashScreen()
                !uiState.authenticated || uiState.profile == null -> LoginScreen(
                    busy = uiState.busy,
                    error = uiState.error,
                    onLogin = viewModel::signIn,
                )

                else -> HomeShell(
                    uiState = uiState,
                    snackbarHostState = snackbarHostState,
                    onRefresh = viewModel::refreshAll,
                    onSignOut = viewModel::signOut,
                    onSetThemeMode = viewModel::setThemeMode,
                    onToggleTheme = viewModel::toggleThemeMode,
                    onSaveInstitute = viewModel::saveInstitute,
                    onSaveClass = viewModel::saveClass,
                    onDeleteClass = viewModel::deleteClass,
                    onDeleteInstitute = viewModel::deleteInstitute,
                    onSaveStaff = viewModel::saveStaff,
                    onDeleteStaff = viewModel::deleteStaff,
                    onSaveStudent = viewModel::saveStudent,
                    onDeleteStudent = viewModel::deleteStudent,
                    onLoadQr = viewModel::fetchQrCode,
                    onScan = viewModel::scanAttendance,
                    onMarkPaid = viewModel::markCurrentMonthPaid,
                    onSkipPayment = viewModel::skipPaymentForScan,
                    onDoneScan = viewModel::clearScanResult,
                    onSaveAttendance = viewModel::saveAttendance,
                    onDeleteAttendance = viewModel::deleteAttendance,
                    onSavePayment = viewModel::savePayment,
                    onDeletePayment = viewModel::deletePayment,
                    onSaveProfile = viewModel::saveProfile,
                )
            }
        }
    }
}

@Composable
private fun SplashScreen() {
    val layout = rememberAdaptiveLayout()
    AdaptiveContentFrame(
        modifier = Modifier
            .fillMaxSize()
            .padding(layout.pagePadding),
        maxWidth = layout.maxContentWidth,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                BrandWordmark(compact = false)
                Text(
                    "Preparing the premium institute dashboard...",
                    color = AppMutedText,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

@Composable
private fun LoginScreen(
    busy: Boolean,
    error: String?,
    onLogin: (String, String) -> Unit,
) {
    val layout = rememberAdaptiveLayout()
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    AdaptiveContentFrame(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = layout.pagePadding, vertical = 24.dp),
        maxWidth = if (layout.preferTwoColumns) 1320.dp else layout.maxContentWidth,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            if (layout.preferTwoColumns) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LoginHero(modifier = Modifier.weight(1f))
                    LoginCard(
                        modifier = Modifier.weight(1f),
                        username = username,
                        password = password,
                        passwordVisible = passwordVisible,
                        busy = busy,
                        error = error,
                        onUsernameChange = { username = it },
                        onPasswordChange = { password = it },
                        onTogglePassword = { passwordVisible = !passwordVisible },
                        onLogin = { onLogin(username, password) },
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(22.dp),
                ) {
                    LoginHero(modifier = Modifier.fillMaxWidth())
                    LoginCard(
                        modifier = Modifier.fillMaxWidth(),
                        username = username,
                        password = password,
                        passwordVisible = passwordVisible,
                        busy = busy,
                        error = error,
                        onUsernameChange = { username = it },
                        onPasswordChange = { password = it },
                        onTogglePassword = { passwordVisible = !passwordVisible },
                        onLogin = { onLogin(username, password) },
                    )
                }
            }
        }
    }
}

@Composable
private fun LoginHero(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BrandWordmark(compact = false, showSubtitle = false)
    }
}

@Composable
private fun LoginInfoPill(label: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = Color.White.copy(alpha = 0.06f),
        border = androidx.compose.foundation.BorderStroke(0.7.dp, Color.White.copy(alpha = 0.12f)),
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            color = AppMutedText,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun LoginCard(
    username: String,
    password: String,
    passwordVisible: Boolean,
    busy: Boolean,
    error: String?,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePassword: () -> Unit,
    onLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassPanel(
        modifier = modifier.widthIn(max = 560.dp),
        shape = RoundedCornerShape(34.dp),
        glowColor = AppPurpleGlow,
        contentPadding = PaddingValues(24.dp),
    ) {
        Text("Sign in", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
        Text(
            "Enter your account details.",
            color = AppMutedText,
        )

        OutlinedTextField(
            value = username,
            onValueChange = onUsernameChange,
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = onTogglePassword) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                    )
                }
            },
        )

        if (!error.isNullOrBlank()) {
            Text(error, color = MaterialTheme.colorScheme.error)
        }

        Button(
            onClick = onLogin,
            modifier = Modifier.fillMaxWidth(),
            enabled = !busy && username.isNotBlank() && password.isNotBlank(),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AppAccent,
                contentColor = Color.Black,
            ),
        ) {
            Text(if (busy) "Signing in..." else "Enter dashboard")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeShell(
    uiState: MainUiState,
    snackbarHostState: SnackbarHostState,
    onRefresh: () -> Unit,
    onSignOut: () -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onToggleTheme: (Boolean) -> Unit,
    onSaveInstitute: (String?, InstituteDraft) -> Unit,
    onSaveClass: (ClassRecord?, ClassDraft) -> Unit,
    onDeleteClass: (String) -> Unit,
    onDeleteInstitute: (String) -> Unit,
    onSaveStaff: (String?, StaffDraft) -> Unit,
    onDeleteStaff: (String) -> Unit,
    onSaveStudent: (Student?, StudentDraft) -> Unit,
    onDeleteStudent: (String) -> Unit,
    onLoadQr: (String, (QrCodeRecord?) -> Unit) -> Unit,
    onScan: (String) -> Unit,
    onMarkPaid: (String) -> Unit,
    onSkipPayment: () -> Unit,
    onDoneScan: () -> Unit,
    onSaveAttendance: (String?, AttendanceDraft) -> Unit,
    onDeleteAttendance: (String) -> Unit,
    onSavePayment: (String?, PaymentDraft) -> Unit,
    onDeletePayment: (String) -> Unit,
    onSaveProfile: (String, String?, String) -> Unit,
) {
    val layout = rememberAdaptiveLayout()
    val destinationHistorySaver = remember {
        listSaver<List<AppDestination>, String>(
            save = { history -> history.map(AppDestination::name) },
            restore = { names ->
                names.mapNotNull { name -> AppDestination.entries.firstOrNull { it.name == name } }
            },
        )
    }
    val availableDestinations = remember(uiState.profile?.role) {
        AppDestination.entries.filter { destination ->
            destination.roles.contains(uiState.profile?.role ?: Role.Staff)
        }
    }
    var selectedDestination by rememberSaveable { mutableStateOf(AppDestination.Dashboard) }
    var destinationHistory by rememberSaveable(stateSaver = destinationHistorySaver) {
        mutableStateOf(emptyList<AppDestination>())
    }
    var showMoreSheet by rememberSaveable { mutableStateOf(false) }
    val profile = uiState.profile
    val workspaceName = remember(profile?.id, uiState.institutes) {
        resolveWorkspaceName(profile, uiState)
    }
    val roleName = if (profile?.role == Role.Admin) "Admin" else "Staff"
    val currentSection = selectedDestination.displayTitle()
    val notificationCount = remember(uiState.students, uiState.payments) { unpaidStudentsThisMonth(uiState) }
    val immersiveScanner = selectedDestination == AppDestination.Attendance && !layout.showNavigationRail
    val useDarkTheme = shouldUseDarkTheme(uiState.themeMode)

    fun navigateTo(destination: AppDestination) {
        if (destination == selectedDestination) {
            showMoreSheet = false
            return
        }
        destinationHistory = (destinationHistory + selectedDestination).takeLast(20)
        selectedDestination = destination
        showMoreSheet = false
    }

    fun navigateBack(): Boolean {
        val previousDestination = destinationHistory.lastOrNull() ?: return false
        destinationHistory = destinationHistory.dropLast(1)
        selectedDestination = previousDestination
        showMoreSheet = false
        return true
    }

    fun exitCurrentFlow() {
        if (!navigateBack()) {
            selectedDestination = AppDestination.Dashboard
        }
    }

    BackHandler(enabled = showMoreSheet || destinationHistory.isNotEmpty()) {
        if (showMoreSheet) {
            showMoreSheet = false
        } else {
            navigateBack()
        }
    }

    LaunchedEffect(availableDestinations) {
        val allowedDestinations = availableDestinations.toSet()
        destinationHistory = destinationHistory.filter { it in allowedDestinations }
        if (selectedDestination !in availableDestinations) {
            selectedDestination = availableDestinations.firstOrNull() ?: AppDestination.Dashboard
        }
    }

    if (showMoreSheet) {
        MoreSheet(
            availableDestinations = availableDestinations,
            selectedDestination = selectedDestination,
            onSelect = ::navigateTo,
            onRefresh = {
                showMoreSheet = false
                onRefresh()
            },
            onSignOut = {
                showMoreSheet = false
                onSignOut()
            },
            onDismiss = { showMoreSheet = false },
        )
    }

    if (layout.showNavigationRail) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 18.dp, top = 18.dp, end = 18.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            SideNavigationRail(
                workspaceName = workspaceName,
                roleName = roleName,
                currentSection = currentSection,
                notificationCount = notificationCount,
                themeMode = uiState.themeMode,
                useDarkTheme = useDarkTheme,
                selectedDestination = selectedDestination,
                availableDestinations = availableDestinations,
                onSelect = ::navigateTo,
                onScan = { navigateTo(AppDestination.Attendance) },
                onToggleTheme = { onToggleTheme(useDarkTheme) },
                onRefresh = onRefresh,
                onSignOut = onSignOut,
            )

            Scaffold(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                containerColor = Color.Transparent,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            ) { innerPadding ->
                AdaptiveContentFrame(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = layout.pagePadding),
                    maxWidth = layout.maxContentWidth,
                ) {
                    ScreenContent(
                        selectedDestination = selectedDestination,
                        uiState = uiState,
                        modifier = Modifier.fillMaxSize(),
                        onSaveInstitute = onSaveInstitute,
                        onSaveClass = onSaveClass,
                        onDeleteClass = onDeleteClass,
                        onDeleteInstitute = onDeleteInstitute,
                        onSaveStaff = onSaveStaff,
                        onDeleteStaff = onDeleteStaff,
                        onSaveStudent = onSaveStudent,
                        onDeleteStudent = onDeleteStudent,
                        onLoadQr = onLoadQr,
                        onScan = onScan,
                        onMarkPaid = onMarkPaid,
                        onSkipPayment = onSkipPayment,
                        onDoneScan = onDoneScan,
                        onSaveAttendance = onSaveAttendance,
                        onDeleteAttendance = onDeleteAttendance,
                        onSavePayment = onSavePayment,
                        onDeletePayment = onDeletePayment,
                        onSaveProfile = onSaveProfile,
                        themeMode = uiState.themeMode,
                        onSetThemeMode = onSetThemeMode,
                        onOpenReports = { navigateTo(AppDestination.Reports) },
                        onOpenStudents = { navigateTo(AppDestination.Students) },
                        onOpenClasses = { navigateTo(AppDestination.Classes) },
                        onOpenProfile = { navigateTo(AppDestination.Profile) },
                        onOpenAttendance = { navigateTo(AppDestination.Attendance) },
                        immersiveScanner = true,
                        onExitScanner = ::exitCurrentFlow,
                    )
                }
            }
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (immersiveScanner) {
            ScreenContent(
                selectedDestination = selectedDestination,
                uiState = uiState,
                modifier = Modifier.fillMaxSize(),
                onSaveInstitute = onSaveInstitute,
                onSaveClass = onSaveClass,
                onDeleteClass = onDeleteClass,
                onDeleteInstitute = onDeleteInstitute,
                onSaveStaff = onSaveStaff,
                onDeleteStaff = onDeleteStaff,
                onSaveStudent = onSaveStudent,
                onDeleteStudent = onDeleteStudent,
                onLoadQr = onLoadQr,
                onScan = onScan,
                onMarkPaid = onMarkPaid,
                onSkipPayment = onSkipPayment,
                onDoneScan = onDoneScan,
                onSaveAttendance = onSaveAttendance,
                onDeleteAttendance = onDeleteAttendance,
                onSavePayment = onSavePayment,
                onDeletePayment = onDeletePayment,
                onSaveProfile = onSaveProfile,
                themeMode = uiState.themeMode,
                onSetThemeMode = onSetThemeMode,
                onOpenReports = { navigateTo(AppDestination.Reports) },
                onOpenStudents = { navigateTo(AppDestination.Students) },
                onOpenClasses = { navigateTo(AppDestination.Classes) },
                onOpenProfile = { navigateTo(AppDestination.Profile) },
                onOpenAttendance = { navigateTo(AppDestination.Attendance) },
                immersiveScanner = true,
                onExitScanner = ::exitCurrentFlow,
            )
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                HomeHeader(
                    workspaceName = workspaceName,
                    roleName = roleName,
                    currentSection = currentSection,
                    notificationCount = notificationCount,
                    themeMode = uiState.themeMode,
                    useDarkTheme = useDarkTheme,
                    onOpenPayments = { navigateTo(AppDestination.Payments) },
                    onRefresh = onRefresh,
                    onToggleTheme = { onToggleTheme(useDarkTheme) },
                    onSignOut = onSignOut,
                )

                AdaptiveContentFrame(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = layout.pagePadding),
                    maxWidth = layout.maxContentWidth,
                ) {
                    ScreenContent(
                        selectedDestination = selectedDestination,
                        uiState = uiState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 132.dp),
                        onSaveInstitute = onSaveInstitute,
                        onSaveClass = onSaveClass,
                        onDeleteClass = onDeleteClass,
                        onDeleteInstitute = onDeleteInstitute,
                        onSaveStaff = onSaveStaff,
                        onDeleteStaff = onDeleteStaff,
                        onSaveStudent = onSaveStudent,
                        onDeleteStudent = onDeleteStudent,
                        onLoadQr = onLoadQr,
                        onScan = onScan,
                        onMarkPaid = onMarkPaid,
                        onSkipPayment = onSkipPayment,
                        onDoneScan = onDoneScan,
                        onSaveAttendance = onSaveAttendance,
                        onDeleteAttendance = onDeleteAttendance,
                        onSavePayment = onSavePayment,
                        onDeletePayment = onDeletePayment,
                        onSaveProfile = onSaveProfile,
                        themeMode = uiState.themeMode,
                        onSetThemeMode = onSetThemeMode,
                        onOpenReports = { navigateTo(AppDestination.Reports) },
                        onOpenStudents = { navigateTo(AppDestination.Students) },
                        onOpenClasses = { navigateTo(AppDestination.Classes) },
                        onOpenProfile = { navigateTo(AppDestination.Profile) },
                        onOpenAttendance = { navigateTo(AppDestination.Attendance) },
                        immersiveScanner = false,
                        onExitScanner = ::exitCurrentFlow,
                    )
                }
            }

            HomeBottomDock(
                modifier = Modifier.align(Alignment.BottomCenter),
                selectedDestination = selectedDestination,
                onRegister = { navigateTo(AppDestination.Students) },
                onScan = { navigateTo(AppDestination.Attendance) },
                onMore = { showMoreSheet = true },
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = if (immersiveScanner) 16.dp else 124.dp),
        )
    }
}

@Composable
private fun ScreenContent(
    selectedDestination: AppDestination,
    uiState: MainUiState,
    modifier: Modifier,
    onSaveInstitute: (String?, InstituteDraft) -> Unit,
    onSaveClass: (ClassRecord?, ClassDraft) -> Unit,
    onDeleteClass: (String) -> Unit,
    onDeleteInstitute: (String) -> Unit,
    onSaveStaff: (String?, StaffDraft) -> Unit,
    onDeleteStaff: (String) -> Unit,
    onSaveStudent: (Student?, StudentDraft) -> Unit,
    onDeleteStudent: (String) -> Unit,
    onLoadQr: (String, (QrCodeRecord?) -> Unit) -> Unit,
    onScan: (String) -> Unit,
    onMarkPaid: (String) -> Unit,
    onSkipPayment: () -> Unit,
    onDoneScan: () -> Unit,
    onSaveAttendance: (String?, AttendanceDraft) -> Unit,
    onDeleteAttendance: (String) -> Unit,
    onSavePayment: (String?, PaymentDraft) -> Unit,
    onDeletePayment: (String) -> Unit,
    onSaveProfile: (String, String?, String) -> Unit,
    themeMode: ThemeMode,
    onSetThemeMode: (ThemeMode) -> Unit,
    onOpenReports: () -> Unit,
    onOpenStudents: () -> Unit,
    onOpenClasses: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenAttendance: () -> Unit,
    immersiveScanner: Boolean,
    onExitScanner: () -> Unit,
) {
    when (selectedDestination) {
        AppDestination.Dashboard -> DashboardScreen(
            uiState = uiState,
            modifier = modifier,
            onOpenReports = onOpenReports,
            onOpenStudents = onOpenStudents,
            onOpenClasses = onOpenClasses,
            onOpenProfile = onOpenProfile,
            onOpenAttendance = onOpenAttendance,
        )

        AppDestination.Institutes -> InstitutesScreen(
            uiState = uiState,
            modifier = modifier,
            onSave = onSaveInstitute,
            onDelete = onDeleteInstitute,
        )

        AppDestination.Staff -> StaffScreen(
            uiState = uiState,
            modifier = modifier,
            onSave = onSaveStaff,
            onDelete = onDeleteStaff,
        )

        AppDestination.Classes -> ClassesScreen(
            uiState = uiState,
            modifier = modifier,
            onSave = onSaveClass,
            onDelete = onDeleteClass,
        )

        AppDestination.Students -> StudentsScreen(
            uiState = uiState,
            modifier = modifier,
            onSave = onSaveStudent,
            onDelete = onDeleteStudent,
            onLoadQr = onLoadQr,
        )

        AppDestination.Attendance -> AttendanceScreen(
            uiState = uiState,
            modifier = modifier,
            onScan = onScan,
            onMarkPaid = onMarkPaid,
            onSkipPayment = onSkipPayment,
            onDoneScan = onDoneScan,
            onSave = onSaveAttendance,
            onDelete = onDeleteAttendance,
            immersive = immersiveScanner,
            onExitImmersive = onExitScanner,
        )

        AppDestination.Payments -> PaymentsScreen(
            uiState = uiState,
            modifier = modifier,
            onSave = onSavePayment,
            onDelete = onDeletePayment,
        )

        AppDestination.Reports -> ReportsScreen(
            uiState = uiState,
            modifier = modifier,
        )

        AppDestination.Help -> HelpScreen(
            uiState = uiState,
            modifier = modifier,
        )

        AppDestination.Profile -> ProfileScreen(
            uiState = uiState,
            modifier = modifier,
            themeMode = themeMode,
            onThemeModeSelected = onSetThemeMode,
            onSave = onSaveProfile,
        )
    }
}

@Composable
private fun HomeHeader(
    workspaceName: String,
    roleName: String,
    currentSection: String,
    notificationCount: Int,
    themeMode: ThemeMode,
    useDarkTheme: Boolean,
    onOpenPayments: () -> Unit,
    onRefresh: () -> Unit,
    onToggleTheme: () -> Unit,
    onSignOut: () -> Unit,
) {
    val layout = rememberAdaptiveLayout()
    val compactHeader = layout.isCompact

    GlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        shape = RoundedCornerShape(30.dp),
        contentPadding = if (compactHeader) {
            PaddingValues(horizontal = 16.dp, vertical = 14.dp)
        } else {
            PaddingValues(horizontal = 18.dp, vertical = 14.dp)
        },
        glowColor = AppPurpleGlow,
    ) {
        if (compactHeader) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.08f),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.AccountCircle,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        workspaceName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        roleName,
                        color = AppMutedText,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HeaderActionIcon(
                        onClick = onToggleTheme,
                        icon = quickThemeToggleIcon(themeMode, useDarkTheme),
                        contentDescription = quickThemeToggleLabel(themeMode, useDarkTheme),
                        containerSize = 40.dp,
                    )
                    HeaderActionIcon(
                        onClick = onRefresh,
                        icon = Icons.Outlined.Refresh,
                        contentDescription = "Refresh data",
                        containerSize = 40.dp,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Color.White.copy(alpha = 0.06f),
                    border = androidx.compose.foundation.BorderStroke(0.7.dp, Color.White.copy(alpha = 0.10f)),
                ) {
                    Text(
                        currentSection,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        color = AppMutedText,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HeaderActionIcon(
                        onClick = onOpenPayments,
                        icon = Icons.Outlined.NotificationsNone,
                        contentDescription = "Payments",
                        badgeCount = notificationCount,
                        containerSize = 40.dp,
                    )
                    HeaderActionIcon(
                        onClick = onSignOut,
                        icon = Icons.AutoMirrored.Outlined.Logout,
                        contentDescription = "Logout",
                        containerSize = 40.dp,
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(52.dp),
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.08f),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.AccountCircle,
                            contentDescription = null,
                            modifier = Modifier.size(30.dp),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        workspaceName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "$roleName  |  $currentSection",
                        color = AppMutedText,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                HeaderActionIcon(
                    onClick = onToggleTheme,
                    icon = quickThemeToggleIcon(themeMode, useDarkTheme),
                    contentDescription = quickThemeToggleLabel(themeMode, useDarkTheme),
                )
                HeaderActionIcon(
                    onClick = onRefresh,
                    icon = Icons.Outlined.Refresh,
                    contentDescription = "Refresh data",
                )
                HeaderActionIcon(
                    onClick = onOpenPayments,
                    icon = Icons.Outlined.NotificationsNone,
                    contentDescription = "Payments",
                    badgeCount = notificationCount,
                )
                HeaderActionIcon(
                    onClick = onSignOut,
                    icon = Icons.AutoMirrored.Outlined.Logout,
                    contentDescription = "Logout",
                )
            }
        }
    }
}

@Composable
private fun HeaderActionIcon(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    badgeCount: Int = 0,
    containerSize: androidx.compose.ui.unit.Dp = 44.dp,
) {
    Box(contentAlignment = Alignment.TopEnd) {
        Surface(
            modifier = Modifier.size(containerSize),
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.06f),
        ) {
            IconButton(onClick = onClick) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        if (badgeCount > 0) {
            Surface(
                shape = CircleShape,
                color = AppDanger,
                modifier = Modifier.padding(top = 1.dp, end = 1.dp),
            ) {
                Text(
                    text = badgeCount.coerceAtMost(99).toString(),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun HomeBottomDock(
    modifier: Modifier = Modifier,
    selectedDestination: AppDestination,
    onRegister: () -> Unit,
    onScan: () -> Unit,
    onMore: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        GlassPanel(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp),
            shape = RoundedCornerShape(28.dp),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
            glowColor = AppOrangeGlow,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                DockAction(
                    label = "Register",
                    icon = Icons.Outlined.PersonAddAlt1,
                    selected = selectedDestination == AppDestination.Students,
                    onClick = onRegister,
                )
                Spacer(modifier = Modifier.width(76.dp))
                DockAction(
                    label = "More",
                    icon = Icons.Outlined.Menu,
                    selected = false,
                    onClick = onMore,
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable(onClick = onScan),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Surface(
                modifier = Modifier.size(66.dp),
                shape = CircleShape,
                color = AppAccent,
                shadowElevation = 16.dp,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(2.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(Color.White.copy(alpha = 0.18f), Color.Transparent),
                            ),
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.QrCodeScanner,
                        contentDescription = "Scan QR",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp),
                    )
                }
            }
            Text(
                "Scan",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun DockAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Surface(
            modifier = Modifier.size(52.dp),
            shape = RoundedCornerShape(18.dp),
            color = if (selected) AppPrimary.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.04f),
            border = androidx.compose.foundation.BorderStroke(
                0.7.dp,
                if (selected) AppPrimary.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.10f),
            ),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (selected) AppPrimary else AppMutedText,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        Text(
            label,
            color = if (selected) MaterialTheme.colorScheme.onSurface else AppMutedText,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SideNavigationRail(
    workspaceName: String,
    roleName: String,
    currentSection: String,
    notificationCount: Int,
    themeMode: ThemeMode,
    useDarkTheme: Boolean,
    selectedDestination: AppDestination,
    availableDestinations: List<AppDestination>,
    onSelect: (AppDestination) -> Unit,
    onScan: () -> Unit,
    onToggleTheme: () -> Unit,
    onRefresh: () -> Unit,
    onSignOut: () -> Unit,
) {
    GlassPanel(
        modifier = Modifier
            .width(268.dp)
            .fillMaxHeight(),
        shape = RoundedCornerShape(34.dp),
        contentPadding = PaddingValues(20.dp),
        glowColor = AppPurpleGlow,
    ) {
        BrandWordmark(compact = true)
        Text("$workspaceName  |  $roleName", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(currentSection, color = AppMutedText, style = MaterialTheme.typography.bodyMedium)

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = AppAccent.copy(alpha = 0.14f),
            border = androidx.compose.foundation.BorderStroke(0.7.dp, AppAccent.copy(alpha = 0.36f)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onScan)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = AppAccent,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.QrCodeScanner, contentDescription = null, tint = Color.White)
                    }
                }
                Column {
                    Text("Scan Hub", fontWeight = FontWeight.Bold)
                    Text("Open the immersive QR flow", color = AppMutedText, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        availableDestinations.forEach { destination ->
            SideRailItem(
                destination = destination,
                selected = destination == selectedDestination,
                badgeCount = if (destination == AppDestination.Payments) notificationCount else 0,
                onClick = { onSelect(destination) },
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        FilledTonalButton(
            onClick = onToggleTheme,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
        ) {
            Icon(quickThemeToggleIcon(themeMode, useDarkTheme), contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(quickThemeToggleLabel(themeMode, useDarkTheme))
        }

        FilledTonalButton(
            onClick = onRefresh,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
        ) {
            Icon(Icons.Outlined.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Refresh")
        }

        Button(
            onClick = onSignOut,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppDanger),
        ) {
            Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Logout")
        }
    }
}

@Composable
private fun SideRailItem(
    destination: AppDestination,
    selected: Boolean,
    badgeCount: Int,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = if (selected) AppPrimary.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.04f),
        border = androidx.compose.foundation.BorderStroke(
            0.7.dp,
            if (selected) AppPrimary.copy(alpha = 0.42f) else Color.White.copy(alpha = 0.10f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(contentAlignment = Alignment.TopEnd) {
                Surface(
                    modifier = Modifier.size(38.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White.copy(alpha = 0.06f),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            destination.icon,
                            contentDescription = destination.displayTitle(),
                            tint = if (selected) AppPrimary else AppMutedText,
                        )
                    }
                }
                if (badgeCount > 0) {
                    Surface(shape = CircleShape, color = AppDanger) {
                        Text(
                            badgeCount.coerceAtMost(99).toString(),
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(destination.displayTitle(), fontWeight = FontWeight.SemiBold)
                Text(destination.sheetDescription(), color = AppMutedText, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoreSheet(
    availableDestinations: List<AppDestination>,
    selectedDestination: AppDestination,
    onSelect: (AppDestination) -> Unit,
    onRefresh: () -> Unit,
    onSignOut: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = AppGlassStrong,
        scrimColor = Color.Black.copy(alpha = 0.54f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Workspace", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
            Text(
                "Every feature stays reachable from here while the home shell remains clean and focused.",
                color = AppMutedText,
            )

            availableDestinations.forEach { destination ->
                SideRailItem(
                    destination = destination,
                    selected = destination == selectedDestination,
                    badgeCount = 0,
                    onClick = { onSelect(destination) },
                )
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.10f))

            FilledTonalButton(
                onClick = onRefresh,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
            ) {
                Icon(Icons.Outlined.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Refresh data")
            }

            Button(
                onClick = onSignOut,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppDanger),
            ) {
                Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Logout")
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun BrandWordmark(compact: Boolean, showSubtitle: Boolean = !compact) {
    val title = buildAnnotatedString {
        pushStyle(SpanStyle(color = AppPrimary, fontWeight = FontWeight.ExtraBold))
        append("UD")
        pop()
        append(" ")
        pushStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.ExtraBold))
        append("Chemistry")
        pop()
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = if (compact) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.displaySmall,
        )
        if (showSubtitle) {
            Text(
                "Luxury institute management for scan-first teams.",
                color = AppMutedText,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

private fun quickThemeToggleIcon(
    themeMode: ThemeMode,
    useDarkTheme: Boolean,
): androidx.compose.ui.graphics.vector.ImageVector {
    return when {
        themeMode == ThemeMode.System && useDarkTheme -> Icons.Outlined.LightMode
        themeMode == ThemeMode.System && !useDarkTheme -> Icons.Outlined.DarkMode
        useDarkTheme -> Icons.Outlined.LightMode
        else -> Icons.Outlined.DarkMode
    }
}

private fun quickThemeToggleLabel(themeMode: ThemeMode, useDarkTheme: Boolean): String {
    return when {
        themeMode == ThemeMode.System && useDarkTheme -> "Switch to light mode"
        themeMode == ThemeMode.System && !useDarkTheme -> "Switch to dark mode"
        useDarkTheme -> "Switch to light mode"
        else -> "Switch to dark mode"
    }
}

private fun resolveWorkspaceName(profile: Profile?, uiState: MainUiState): String {
    if (profile == null) return "UDChemistry"
    if (profile.role == Role.Admin) return "UDChemistry"
    return uiState.institutes.firstOrNull { it.id == profile.instituteId }?.name ?: "Institute"
}

private fun unpaidStudentsThisMonth(uiState: MainUiState): Int {
    val today = LocalDate.now()
    val paidStudentIds = uiState.payments
        .filter { it.paid && it.paymentMonth == today.monthValue && it.paymentYear == today.year }
        .map { it.studentId }
        .toSet()
    return uiState.students.count { it.id !in paidStudentIds }
}

private fun AppDestination.displayTitle(): String = when (this) {
    AppDestination.Reports -> "Summary"
    AppDestination.Help -> "Help"
    AppDestination.Profile -> "Settings"
    else -> title
}

private fun AppDestination.sheetDescription(): String = when (this) {
    AppDestination.Dashboard -> "Live revenue, attendance, and scan shortcuts."
    AppDestination.Institutes -> "Manage institute records and status."
    AppDestination.Staff -> "Create and update staff accounts."
    AppDestination.Classes -> "Plan weekly and extra class schedules."
    AppDestination.Students -> "Register students, QR sharing, and insights."
    AppDestination.Attendance -> "Immersive QR scanning and manual corrections."
    AppDestination.Payments -> "Track monthly payment status."
    AppDestination.Reports -> "Export summaries, CSV, and PDF reports."
    AppDestination.Help -> "Read quick instructions for sign-in, dashboard, scanning, and records."
    AppDestination.Profile -> "Update account details and password."
}

@Preview(name = "Login Preview", showBackground = true, backgroundColor = 0xFF060814)
@Composable
private fun LoginScreenPreview() {
    UDChemistryTheme(themeMode = ThemeMode.Dark) {
        AppGradientBackground {
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
                LoginScreen(
                    busy = false,
                    error = null,
                    onLogin = { _, _ -> },
                )
            }
        }
    }
}

@Preview(name = "Home Preview", showBackground = true, backgroundColor = 0xFF060814)
@Composable
private fun HomeShellPreview() {
    UDChemistryTheme(themeMode = ThemeMode.Dark) {
        AppGradientBackground {
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
                HomeShell(
                    uiState = previewMainUiState(),
                    snackbarHostState = remember { SnackbarHostState() },
                    onRefresh = {},
                    onSignOut = {},
                    onSetThemeMode = {},
                    onToggleTheme = {},
                    onSaveInstitute = { _, _ -> },
                    onSaveClass = { _, _ -> },
                    onDeleteClass = {},
                    onDeleteInstitute = {},
                    onSaveStaff = { _, _ -> },
                    onDeleteStaff = {},
                    onSaveStudent = { _, _ -> },
                    onDeleteStudent = {},
                    onLoadQr = { _, onLoaded -> onLoaded(null) },
                    onScan = {},
                    onMarkPaid = {},
                    onSkipPayment = {},
                    onDoneScan = {},
                    onSaveAttendance = { _, _ -> },
                    onDeleteAttendance = {},
                    onSavePayment = { _, _ -> },
                    onDeletePayment = {},
                    onSaveProfile = { _, _, _ -> },
                )
            }
        }
    }
}
