package com.udchemistry.mobile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.udchemistry.mobile.data.ChemistryRepository
import com.udchemistry.mobile.data.SessionExpiredException
import com.udchemistry.mobile.data.ThemeStore
import com.udchemistry.mobile.model.AttendanceDraft
import com.udchemistry.mobile.model.AttendanceRecord
import com.udchemistry.mobile.model.ClassDraft
import com.udchemistry.mobile.model.ClassRecord
import com.udchemistry.mobile.model.DashboardData
import com.udchemistry.mobile.model.Institute
import com.udchemistry.mobile.model.InstituteDraft
import com.udchemistry.mobile.model.PaymentDraft
import com.udchemistry.mobile.model.PaymentRecord
import com.udchemistry.mobile.model.Profile
import com.udchemistry.mobile.model.QrCodeRecord
import com.udchemistry.mobile.model.ScanAttendanceResult
import com.udchemistry.mobile.model.StaffDraft
import com.udchemistry.mobile.model.Student
import com.udchemistry.mobile.model.StudentDraft
import com.udchemistry.mobile.ui.theme.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MainUiState(
    val themeMode: ThemeMode = ThemeMode.Dark,
    val bootstrapping: Boolean = true,
    val authenticated: Boolean = false,
    val profile: Profile? = null,
    val institutes: List<Institute> = emptyList(),
    val classes: List<ClassRecord> = emptyList(),
    val staff: List<Profile> = emptyList(),
    val students: List<Student> = emptyList(),
    val qrCodes: List<QrCodeRecord> = emptyList(),
    val attendance: List<AttendanceRecord> = emptyList(),
    val payments: List<PaymentRecord> = emptyList(),
    val dashboard: DashboardData = DashboardData(),
    val scanResult: ScanAttendanceResult? = null,
    val scanPaymentStatus: String = "idle",
    val busy: Boolean = false,
    val refreshing: Boolean = false,
    val notice: String? = null,
    val error: String? = null,
)

class MainViewModel(
    private val repository: ChemistryRepository,
    private val themeStore: ThemeStore,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        observeThemeMode()
        initialize()
    }

    private fun observeThemeMode() {
        viewModelScope.launch {
            themeStore.themeMode.collectLatest { themeMode ->
                _uiState.update { it.copy(themeMode = themeMode) }
            }
        }
    }

    fun initialize() {
        viewModelScope.launch {
            _uiState.update { it.copy(bootstrapping = true, error = null, notice = null) }
            try {
                val profile = withContext(Dispatchers.IO) { repository.restoreSession() }
                if (profile == null) {
                    _uiState.update { state -> MainUiState(themeMode = state.themeMode, bootstrapping = false) }
                } else {
                    refreshAllInternal(profile, bootstrapping = true)
                }
            } catch (error: Exception) {
                _uiState.update {
                    MainUiState(
                        themeMode = it.themeMode,
                        bootstrapping = false,
                        error = error.message ?: "Unable to restore the mobile session.",
                    )
                }
            }
        }
    }

    fun signIn(username: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true, error = null, notice = null) }
            try {
                val profile = withContext(Dispatchers.IO) { repository.signIn(username, password) }
                refreshAllInternal(profile, bootstrapping = false)
                _uiState.update { it.copy(notice = "Welcome back.") }
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        busy = false,
                        bootstrapping = false,
                        error = error.message ?: "Unable to sign in.",
                    )
                }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.signOut() }
            _uiState.value = MainUiState(
                themeMode = _uiState.value.themeMode,
                bootstrapping = false,
                notice = "Signed out successfully.",
            )
        }
    }

    fun refreshAll() {
        val profile = _uiState.value.profile ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(refreshing = true, error = null) }
            try {
                refreshAllInternal(profile, bootstrapping = false)
            } catch (error: Exception) {
                handleFailure(error)
            }
        }
    }

    fun saveInstitute(editingId: String?, draft: InstituteDraft) {
        mutateAndRefresh("Institute saved successfully.") {
            if (editingId == null) repository.createInstitute(draft) else repository.updateInstitute(editingId, draft)
        }
    }

    fun saveClass(editingClass: ClassRecord?, draft: ClassDraft) {
        mutateAndRefresh(if (editingClass == null) "Class saved successfully." else "Class updated successfully.") {
            val profile = requireProfile()
            if (editingClass == null) repository.createClass(profile, draft) else repository.updateClass(profile, editingClass.id, draft)
        }
    }

    fun deleteClass(id: String) {
        mutateAndRefresh("Class deleted successfully.") {
            repository.deleteClass(id)
        }
    }

    fun deleteInstitute(id: String) {
        mutateAndRefresh("Institute deleted successfully.") {
            repository.deleteInstitute(id)
        }
    }

    fun saveStaff(editingId: String?, draft: StaffDraft) {
        mutateAndRefresh("Staff account saved successfully.") {
            if (editingId == null) repository.createStaff(draft) else repository.updateStaff(editingId, draft)
        }
    }

    fun deleteStaff(id: String) {
        mutateAndRefresh("Staff account deleted successfully.") {
            repository.deleteStaff(id)
        }
    }

    fun saveStudent(editingStudent: Student?, draft: StudentDraft) {
        mutateAndRefresh(if (editingStudent == null) "Student added successfully." else "Student updated successfully.") {
            val profile = requireProfile()
            if (editingStudent == null) repository.createStudent(profile, draft) else repository.updateStudent(editingStudent, draft)
        }
    }

    fun deleteStudent(id: String) {
        mutateAndRefresh("Student deleted successfully.") {
            repository.deleteStudent(id)
        }
    }

    fun saveAttendance(editingId: String?, draft: AttendanceDraft) {
        mutateAndRefresh("Attendance saved successfully.") {
            val profile = requireProfile()
            if (editingId == null) repository.createAttendance(profile, draft) else repository.updateAttendance(profile, editingId, draft)
        }
    }

    fun deleteAttendance(id: String) {
        mutateAndRefresh("Attendance deleted successfully.") {
            repository.deleteAttendance(id)
        }
    }

    fun savePayment(editingId: String?, draft: PaymentDraft) {
        mutateAndRefresh("Payment saved successfully.") {
            val profile = requireProfile()
            if (editingId == null) repository.createPayment(profile, draft) else repository.updatePayment(profile, editingId, draft)
        }
    }

    fun deletePayment(id: String) {
        mutateAndRefresh("Payment deleted successfully.") {
            repository.deletePayment(id)
        }
    }

    fun scanAttendance(qrValue: String) {
        mutateAndRefresh(notice = null, refreshData = true) {
            val profile = requireProfile()
            val result = repository.scanAttendance(profile, qrValue)
            _uiState.update {
                it.copy(
                    scanResult = result,
                    scanPaymentStatus = "idle",
                    notice = if (result.duplicate) "Attendance was already recorded for today." else "Attendance marked successfully.",
                )
            }
        }
    }

    fun markCurrentMonthPaid(studentId: String) {
        mutateAndRefresh(notice = "Payment marked for the current month.", refreshData = true) {
            val profile = requireProfile()
            repository.markCurrentMonthPaid(profile, studentId)
            _uiState.update { it.copy(scanPaymentStatus = "paid") }
        }
    }

    fun skipPaymentForScan() {
        _uiState.update {
            it.copy(scanPaymentStatus = "skipped", notice = "Payment was skipped for this scan.")
        }
    }

    fun clearScanResult() {
        _uiState.update { it.copy(scanResult = null, scanPaymentStatus = "idle") }
    }

    fun saveProfile(fullName: String, phone: String?, password: String) {
        mutateAndRefresh("Profile updated successfully.") {
            val profile = requireProfile()
            repository.updateProfile(profile.id, fullName, phone)
            if (password.isNotBlank()) {
                repository.updatePassword(password)
            }
        }
    }

    fun clearFeedback() {
        _uiState.update { it.copy(notice = null, error = null) }
    }

    fun setThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            themeStore.save(themeMode)
        }
    }

    fun toggleThemeMode(resolvedDarkTheme: Boolean) {
        setThemeMode(if (resolvedDarkTheme) ThemeMode.Light else ThemeMode.Dark)
    }

    fun fetchQrCode(studentId: String, onLoaded: (QrCodeRecord?) -> Unit) {
        viewModelScope.launch {
            try {
                val qrCode = withContext(Dispatchers.IO) { repository.fetchQrCodeForStudent(studentId) }
                onLoaded(qrCode)
            } catch (_: Exception) {
                onLoaded(null)
            }
        }
    }

    private fun mutateAndRefresh(
        notice: String?,
        refreshData: Boolean = true,
        block: suspend () -> Unit,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true, error = null) }
            try {
                withContext(Dispatchers.IO) { block() }
                val profile = requireProfile()
                if (refreshData) {
                    refreshAllInternal(profile, bootstrapping = false)
                }
                _uiState.update { it.copy(busy = false, refreshing = false, notice = notice ?: it.notice) }
            } catch (error: Exception) {
                handleFailure(error)
            }
        }
    }

    private suspend fun refreshAllInternal(profile: Profile, bootstrapping: Boolean) {
        val snapshot = withContext(Dispatchers.IO) { repository.loadSnapshot(profile) }
        _uiState.value = _uiState.value.copy(
            bootstrapping = false,
            authenticated = true,
            busy = false,
            refreshing = false,
            profile = profile,
            institutes = snapshot.institutes,
            classes = snapshot.classes,
            staff = snapshot.staff,
            students = snapshot.students,
            qrCodes = snapshot.qrCodes,
            attendance = snapshot.attendance,
            payments = snapshot.payments,
            dashboard = snapshot.dashboard,
            error = null,
        )
    }

    private suspend fun requireProfile(): Profile {
        return _uiState.value.profile ?: throw SessionExpiredException("Please sign in again.")
    }

    private suspend fun handleFailure(error: Exception) {
        if (error is SessionExpiredException) {
            repository.signOut()
            _uiState.value = MainUiState(
                themeMode = _uiState.value.themeMode,
                bootstrapping = false,
                error = error.message ?: "Session expired. Please sign in again.",
            )
            return
        }

        _uiState.update {
            it.copy(
                bootstrapping = false,
                busy = false,
                refreshing = false,
                error = error.message ?: "Something went wrong.",
            )
        }
    }

    override fun onCleared() {
        repository.close()
        super.onCleared()
    }
}

class MainViewModelFactory(
    private val repository: ChemistryRepository,
    private val themeStore: ThemeStore,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository, themeStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
