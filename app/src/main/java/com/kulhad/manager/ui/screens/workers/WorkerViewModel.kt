package com.kulhad.manager.ui.screens.workers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kulhad.manager.data.local.entity.WorkerType
import com.kulhad.manager.data.repository.WorkerRepository
import com.kulhad.manager.di.WorkingDateManager
import com.kulhad.manager.data.util.toDisplay
import com.kulhad.manager.domain.model.AttendanceRecord
import com.kulhad.manager.domain.model.AuditDisplay
import com.kulhad.manager.domain.model.Worker
import com.kulhad.manager.domain.model.WorkerAdvanceRecord
import com.kulhad.manager.domain.model.WorkerTypeChange
import com.kulhad.manager.domain.model.WorkerWithAttendance
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class WorkerFilter { ALL, PIECE, SALARY }

data class WorkerListData(
    val workers: List<WorkerWithAttendance>,
    val totalCount: Int,
    val presentCount: Int,
    val absentCount: Int
)

// ── Attendance history UI models ─────────────────────────────────────────────

/**
 * A single row displayed on the AttendanceHistoryScreen.
 *
 * [audit] is the presentation-layer view of the row's write-audit metadata.
 * Passed to [com.kulhad.manager.ui.components.AuditInfoCard] in the edit dialog.
 */
data class AttendanceUi(
    val workerId: Long,
    val workerName: String,
    val isPresent: Boolean,
    val date: Long,
    val audit: AuditDisplay
)

/** Full UI state for AttendanceHistoryScreen. */
data class AttendanceHistoryUiState(
    val selectedWorkerId: Long? = null,
    val attendance: List<AttendanceUi> = emptyList()
)

// ── Attendance screen UI state ───────────────────────────────────────────────

/** A single inactive worker row shown read-only on past-date attendance views. */
data class InactiveWorkerAttendance(
    val worker: Worker,
    val isPresent: Boolean
)

/**
 * Unified state for [AttendanceScreen].
 *
 * [isCurrentDate] — true when the selected working date equals today.
 * [activeWorkers] — currently active workers; always shown, always editable.
 * [inactiveAttendanceRows] — inactive workers who have a saved record on the selected
 *   date; only populated for past dates (always empty when [isCurrentDate] is true).
 * [attendanceMap] — saved DB state (workerId → isPresent) for all workers on the date.
 */
data class AttendanceScreenState(
    val isCurrentDate: Boolean = true,
    val activeWorkers: List<Worker> = emptyList(),
    val inactiveAttendanceRows: List<InactiveWorkerAttendance> = emptyList(),
    val attendanceMap: Map<Long, Boolean> = emptyMap()
)

@HiltViewModel
class WorkerViewModel @Inject constructor(
    private val repository: WorkerRepository,
    private val workingDateManager: WorkingDateManager
) : ViewModel() {

    // ── Global working date ──────────────────────────────────────────────────
    /**
     * The process-scoped working date from [WorkingDateManager].
     * Delegates the same StateFlow — no state is duplicated.
     * Attendance saves and advance inserts use [workingDateManager.currentEpochMilli] internally.
     */
    val workingDate: StateFlow<LocalDate> = workingDateManager.currentWorkingDate

    /** Forwards date selection to [WorkingDateManager]; future dates are silently rejected. */
    fun setWorkingDate(date: LocalDate) = workingDateManager.setWorkingDate(date)

    private val _filter = MutableStateFlow(WorkerFilter.ALL)
    val filter: StateFlow<WorkerFilter> = _filter.asStateFlow()

    val listData: StateFlow<WorkerListData> = combine(
        // Active workers only — WorkerListScreen is an operational screen.
        // Inactive workers are accessible via the Workers Archive screen.
        repository.observeActiveWorkersWithTodayAttendance(),
        _filter
    ) { active, filter ->
        val filtered = when (filter) {
            WorkerFilter.ALL    -> active
            WorkerFilter.PIECE  -> active.filter { it.worker.currentType == WorkerType.PIECE }
            WorkerFilter.SALARY -> active.filter { it.worker.currentType == WorkerType.SALARY }
        }
        WorkerListData(
            workers      = filtered,
            totalCount   = active.size,
            presentCount = active.count { it.isPresentToday == true },
            absentCount  = active.count { it.isPresentToday == false }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkerListData(emptyList(), 0, 0, 0))

    fun setFilter(filter: WorkerFilter) {
        _filter.value = filter
    }

    fun observeWorker(id: Long) = repository.observeWorker(id)

    fun saveWorker(
        existingId: Long?,
        name: String,
        phone: String,
        address: String,
        joiningDate: Long,
        type: WorkerType,
        dailyRate: Int,
        isActive: Boolean = true,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                if (existingId == null) {
                    // New workers are always active; isActive param is intentionally ignored here.
                    repository.saveNewWorker(name, phone, address, joiningDate, type, dailyRate)
                } else {
                    // Single atomic call: profile update (including isActive) + conditional
                    // type/history change in one withTransaction.
                    repository.saveWorkerEdit(
                        workerId     = existingId,
                        name         = name,
                        phone        = phone,
                        address      = address,
                        joiningDate  = joiningDate,
                        isActive     = isActive,
                        newType      = type,
                        newDailyRate = dailyRate
                    )
                }
                onDone()
            } catch (_: Exception) { /* keep UI on form */ }
        }
    }

    fun observeTypeHistory(workerId: Long): kotlinx.coroutines.flow.Flow<List<WorkerTypeChange>> =
        repository.observeTypeHistory(workerId)

    fun changeType(
        workerId: Long,
        newType: WorkerType,
        dailyRate: Int,
        effectiveFrom: Long,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                repository.changeType(workerId, newType, dailyRate, effectiveFrom)
                onDone()
            } catch (_: Exception) {}
        }
    }

    // ── Activate / Deactivate ────────────────────────────────────────────────

    fun setWorkerActive(workerId: Long, isActive: Boolean) {
        viewModelScope.launch {
            try { repository.setWorkerActive(workerId, isActive) } catch (_: Exception) {}
        }
    }

    // Attendance --------------------------------------------------------------

    /** Active workers only — used in operational entry screens (attendance, advance, production). */
    val activeWorkers: StateFlow<List<Worker>> = repository.observeActiveWorkers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * ALL workers (active + inactive) — used in history / report screens and for
     * worker name resolution so deactivated workers still appear by name in past records.
     */
    val allWorkers: StateFlow<List<Worker>> = repository.observeAllWorkers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Present count for the currently selected working date.
     *
     * Re-queries whenever the working date changes via [WorkingDateManager].
     * [flatMapLatest] cancels the previous DB subscription the moment a new date is selected.
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val attendancePresentToday: StateFlow<Int> =
        workingDateManager.currentWorkingDate
            .flatMapLatest { date ->
                val epochMilli = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                repository.observePresentCountForDate(epochMilli)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /**
     * Absent count for the currently selected working date.
     *
     * Same date-reactive pattern as [attendancePresentToday].
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val attendanceAbsentToday: StateFlow<Int> =
        workingDateManager.currentWorkingDate
            .flatMapLatest { date ->
                val epochMilli = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                repository.observeAbsentCountForDate(epochMilli)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val attendanceTrend: StateFlow<List<Pair<Long, Int>>> = repository.observeAttendanceTrend(7)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Attendance state for the currently selected working date.
     *
     * Re-queries whenever the working date changes via [WorkingDateManager].
     * Uses [flatMapLatest] so the previous DB subscription is cancelled the moment a new
     * date is selected — no stale data from the old date can leak through.
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val attendanceDateMap: StateFlow<Map<Long, Boolean>> =
        workingDateManager.currentWorkingDate
            .flatMapLatest { date ->
                val epochMilli = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                repository.observeAttendanceForDate(epochMilli)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /**
     * Unified state for [AttendanceScreen].
     *
     * For the current date: [AttendanceScreenState.inactiveAttendanceRows] is always
     * empty — only active workers are shown and editable.
     *
     * For past dates: inactive workers who have a saved attendance record on that date
     * are surfaced as read-only rows in [AttendanceScreenState.inactiveAttendanceRows].
     * They are identified by joining [repository.observeAllWorkers] with the date's
     * attendance map — workers absent from [allWorkers] or currently active are excluded.
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val attendanceScreenState: StateFlow<AttendanceScreenState> =
        workingDateManager.currentWorkingDate
            .flatMapLatest { date ->
                val epochMilli = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val isCurrentDate = date.isEqual(LocalDate.now())
                combine(
                    repository.observeActiveWorkers(),
                    repository.observeAllWorkers(),
                    repository.observeAttendanceForDate(epochMilli)
                ) { activeWorkers, allWorkers, attendanceMap ->
                    val inactiveWithRecords: List<InactiveWorkerAttendance> =
                        if (isCurrentDate) emptyList()
                        else {
                            val workerMap = allWorkers.associateBy { it.id }
                            attendanceMap.entries
                                .mapNotNull { (workerId, isPresent) ->
                                    val w = workerMap[workerId]
                                    if (w != null && !w.isActive) {
                                        InactiveWorkerAttendance(worker = w, isPresent = isPresent)
                                    } else null
                                }
                                .sortedBy { it.worker.name }
                        }
                    AttendanceScreenState(
                        isCurrentDate          = isCurrentDate,
                        activeWorkers          = activeWorkers,
                        inactiveAttendanceRows = inactiveWithRecords,
                        attendanceMap          = attendanceMap
                    )
                }
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                AttendanceScreenState()
            )

    fun saveAttendance(presence: Map<Long, Boolean>, onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                repository.saveAttendanceBatch(workingDateManager.currentEpochMilli(), presence)
                onDone()
            } catch (_: Exception) {}
        }
    }

    // Attendance history ------------------------------------------------------

    /**
     * Worker ID filter for AttendanceHistoryScreen.
     * null = All Workers, non-null = single worker.
     * Scoped to the ViewModel lifetime (i.e., this NavBackStackEntry).
     */
    private val _historyWorkerFilter = MutableStateFlow<Long?>(null)
    val historyWorkerFilter: StateFlow<Long?> = _historyWorkerFilter.asStateFlow()

    /**
     * Reactive attendance history: re-queries whenever the global working date OR
     * the worker filter changes. Uses [flatMapLatest] so in-flight queries are
     * automatically cancelled on filter/date changes.
     *
     * Worker names are resolved by combining with [allWorkers] so deactivated workers
     * still appear by name in historical records rather than falling back to "Worker".
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val attendanceHistory: StateFlow<AttendanceHistoryUiState> = combine(
        workingDateManager.currentWorkingDate,
        _historyWorkerFilter
    ) { date, workerId -> date to workerId }
        .flatMapLatest { (date, workerId) ->
            val dateMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            combine(
                repository.observeAttendanceHistory(dateMillis, workerId),
                // Use allWorkers (not activeWorkers) so deactivated workers still resolve
                // to their name in historical attendance records.
                allWorkers
            ) { records: List<AttendanceRecord>, workers: List<Worker> ->
                val workerMap = workers.associateBy { it.id }
                AttendanceHistoryUiState(
                    selectedWorkerId = workerId,
                    attendance = records.map { rec ->
                        AttendanceUi(
                            workerId   = rec.workerId,
                            workerName = workerMap[rec.workerId]?.name ?: "Worker",
                            isPresent  = rec.isPresent,
                            date       = rec.date,
                            audit      = rec.audit.toDisplay()
                        )
                    }
                )
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            AttendanceHistoryUiState()
        )

    /** Switches the worker filter; null clears back to "All Workers". */
    fun setWorkerFilter(workerId: Long?) {
        _historyWorkerFilter.value = workerId
    }

    /**
     * Updates an existing attendance row to [isPresent].
     *
     * Delegates to [WorkerRepository.editAttendance] which issues a targeted SQL UPDATE
     * — structurally cannot insert a duplicate row.
     */
    fun updateAttendance(workerId: Long, date: Long, isPresent: Boolean) {
        viewModelScope.launch {
            try {
                repository.editAttendance(workerId, date, isPresent)
            } catch (_: Exception) {}
        }
    }

    // Advances ----------------------------------------------------------------

    fun observeAdvances(workerId: Long): kotlinx.coroutines.flow.Flow<List<WorkerAdvanceRecord>> =
        repository.observeAdvances(workerId)

    fun observeAdvanceTotalThisMonth(workerId: Long) =
        repository.observeAdvanceTotalThisMonth(workerId)

    fun saveAdvance(
        workerId: Long,
        amount: Int,
        remark: String,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                repository.saveAdvance(workerId, amount, workingDateManager.currentEpochMilli(), remark)
                onDone()
            } catch (_: Exception) {}
        }
    }

    // Helpers -----------------------------------------------------------------

    fun observeWorkerName(id: Long) = repository.observeWorker(id).map { it?.name ?: "Worker" }
}
