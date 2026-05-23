package com.kulhad.manager.ui.screens.workers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kulhad.manager.data.local.entity.WorkerType
import com.kulhad.manager.data.repository.WorkerRepository
import com.kulhad.manager.data.util.DateUtils
import com.kulhad.manager.domain.model.Worker
import com.kulhad.manager.domain.model.WorkerAdvanceRecord
import com.kulhad.manager.domain.model.WorkerTypeChange
import com.kulhad.manager.domain.model.WorkerWithAttendance
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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

@HiltViewModel
class WorkerViewModel @Inject constructor(
    private val repository: WorkerRepository
) : ViewModel() {

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

    val attendanceTodayMap: StateFlow<Map<Long, Boolean>> =
        repository.observeAttendanceForDate(DateUtils.todayStart())
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun saveAttendance(presence: Map<Long, Boolean>, onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                repository.saveAttendanceBatch(DateUtils.todayStart(), presence)
                onDone()
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
        date: Long,
        remark: String,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                repository.saveAdvance(workerId, amount, date, remark)
                onDone()
            } catch (_: Exception) {}
        }
    }

    // Helpers -----------------------------------------------------------------

    fun observeWorkerName(id: Long) = repository.observeWorker(id).map { it?.name ?: "Worker" }
}
