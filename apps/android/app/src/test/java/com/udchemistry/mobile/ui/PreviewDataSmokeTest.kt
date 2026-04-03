package com.udchemistry.mobile.ui

import com.udchemistry.mobile.model.Role
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PreviewDataSmokeTest {

    @Test
    fun adminPreviewContainsClassesStudentsAndMonthlyFees() {
        val state = previewMainUiState(Role.Admin)

        assertTrue(state.authenticated)
        assertEquals(Role.Admin, state.profile?.role)
        assertEquals(3, state.classes.size)
        assertEquals(4, state.students.size)
        assertTrue(state.classes.any { it.classType == "general" })
        assertTrue(state.classes.any { it.classType == "extra" })
        assertTrue(state.students.all { it.monthlyFee > 0.0 })
        assertEquals(
            state.students.size,
            state.dashboard.metrics.firstOrNull { it.label == "Total students" }?.value,
        )
    }

    @Test
    fun staffPreviewKeepsInstituteScopeAndClassRoster() {
        val state = previewMainUiState(Role.Staff)

        assertTrue(state.authenticated)
        assertEquals(Role.Staff, state.profile?.role)
        assertNotNull(state.profile?.instituteId)
        assertFalse(state.profile?.instituteId.isNullOrBlank())
        assertTrue(state.classes.isNotEmpty())
        assertTrue(state.classes.all { it.instituteId == state.profile?.instituteId })
        assertTrue(state.students.all { it.instituteId == state.profile?.instituteId })
    }
}
