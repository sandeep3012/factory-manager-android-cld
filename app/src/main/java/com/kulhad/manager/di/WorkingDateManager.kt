package com.kulhad.manager.di

import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Process-scoped holder for the globally selected working date.
 *
 * ── Lifecycle guarantee ──────────────────────────────────────────────────────
 * Survives navigation  → same OS process → same @Singleton instance → state kept
 * Resets on app launch → process death/restart → new instance → LocalDate.now()
 *
 * No persistence (DataStore / SharedPrefs) is intentional: process death is the
 * natural reset boundary. The date must NOT survive app restarts.
 *
 * ── Invariant ────────────────────────────────────────────────────────────────
 * currentWorkingDate.value is ALWAYS ≤ today. The guard in [setWorkingDate]
 * enforces this even if the UI somehow bypasses the picker restriction.
 *
 * ── Consumers ────────────────────────────────────────────────────────────────
 * WorkerViewModel    (AttendanceScreen, AdvanceEntryScreen)
 * ProductionViewModel (ProductionScreen, AddProductionScreen)
 * SalesViewModel     (SalesScreen, CreateSaleScreen, PaymentEntryScreen)
 *
 * Each ViewModel delegates the StateFlow directly — no copied state elsewhere.
 */
@Singleton
class WorkingDateManager @Inject constructor() {

    private val _currentWorkingDate = MutableStateFlow(LocalDate.now())

    /** The globally selected working date. Always ≤ today. Read-only from UI. */
    val currentWorkingDate: StateFlow<LocalDate> = _currentWorkingDate.asStateFlow()

    /**
     * Sets the working date.
     *
     * Silently ignored when [date] is in the future — the DatePicker already blocks
     * future selection, but this guard keeps the invariant intact regardless of caller.
     */
    fun setWorkingDate(date: LocalDate) {
        if (!isFutureDate(date)) {
            _currentWorkingDate.value = date
        }
    }

    /** Resets the working date to today. */
    fun resetToToday() {
        _currentWorkingDate.value = LocalDate.now()
    }

    /** Returns true when [date] is strictly after today. */
    fun isFutureDate(date: LocalDate): Boolean = date.isAfter(LocalDate.now())

    /**
     * Returns the working date as start-of-day epoch millis in the device's default
     * time zone. Use this value when passing the working date to Room queries or inserts.
     *
     * Phase 2 note: ViewModels and repositories will call this instead of
     * [com.kulhad.manager.data.util.DateUtils.todayStart] once the working date is
     * fully wired through each module.
     */
    fun currentEpochMilli(): Long =
        _currentWorkingDate.value
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
}
