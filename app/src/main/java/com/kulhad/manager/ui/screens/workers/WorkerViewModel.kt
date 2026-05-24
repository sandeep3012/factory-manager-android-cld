package com.kulhad.manager.ui.screens.workers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kulhad.manager.data.local.entity.WorkerType
import com.kulhad.manager.data.repository.WorkerRepository
import com.kulhad.manager.di.WorkingDateManager
import com.kulhad.manager.domain.model.AttendanceRecord
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

/** A single row displayed on the AttendanceHistoryScreen. */
data class AttendanceUi(
    val workerId: Long,
    val workerName: String,
    val isPresent: Boolean,
    val date: Long
)

/** Full UI state for AttendanceHistoryScreen. */
data class AttendanceHistoryUiState(
    val selectedWorkerId: Long? = null,
    val attendance: List<AttendanceUi> = emptyList()
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
        repository.observeWorkersWithTodayAttendance(),
        _filter
    ) { all, filter ->
        val filtered = when (filter) {
            WorkerFilter.ALL -> all
            WorkerFilter.PIECE -> all.filter { it.worker.currentType == WorkerType.PIECE }
            WorkerFilter.SALARY -> all.filter { it.worker.currentType == WorkerType.SALARY }
        }
        WorkerListData(
            workers = filtered,
            totalCount = all.size,
            presentCount = all.count { it.isPresentToday == true },
            absentCount = all.count { it.isPresentToday == false }
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
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                if (existingId == null) {
                    repository.saveNewWorker(name, phone, address, joiningDate, type, dailyRate)
                } else {
                    // Single atomic call: profile update + conditional type/history change
                    // in one withTransaction. It is now structurally impossible for this
                    // path to update currentType/dailyRate without creating a history row.
                    repository.saveWorkerEdit(
                        workerId     = existingId,
                        name         = name,
                        phone        = phone,
                        address      = address,
                        joiningDate  = joiningDate,
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

    // Attendance --------------------------------------------------------------

    val activeWorkers: StateFlow<List<Worker>> = repository.observeActiveWorkers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val attendancePresentToday: StateFlow<Int> = repository.observePresentCountToday()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val attendanceAbsentToday: StateFlow<Int> = repository.observeAbsentCountToday()
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
     * Worker names are resolved by combining with [activeWorkers]; workers not found
     * in the active list fall back to "Worker" so stale records remain visible.
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
                activeWorkers
            ) { records: List<AttendanceRecord>, workers: List<Worker> ->
                val workerMap = workers.associateBy { it.id }
                AttendanceHistoryUiState(
                    selectedWorkerId = workerId,
                    attendance = records.map { rec ->
                        AttendanceUi(
                            workerId   = rec.workerId,
                            workerName = workerMap[rec.workerId]?.name ?: "Worker",
                            isPresent  = rec.isPresent,
                            date       = rec.date
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
