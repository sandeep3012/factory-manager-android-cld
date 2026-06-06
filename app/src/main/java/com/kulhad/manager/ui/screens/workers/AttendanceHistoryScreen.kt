package com.kulhad.manager.ui.screens.workers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.kulhad.manager.data.local.entity.WorkerType
import com.kulhad.manager.data.repository.WorkerMonthAttendance
import com.kulhad.manager.data.repository.WorkerRepository
import com.kulhad.manager.data.util.DateUtils
import com.kulhad.manager.ui.components.BadgeType
import com.kulhad.manager.ui.components.KpiStrip
import com.kulhad.manager.ui.components.KulhadTopBar
import com.kulhad.manager.ui.components.SegmentedControl
import com.kulhad.manager.ui.components.StatusBadge
import com.kulhad.manager.ui.components.WorkerAvatar
import com.kulhad.manager.ui.theme.BgDeep
import com.kulhad.manager.ui.theme.ErrorRed
import com.kulhad.manager.ui.theme.InfoBlue
import com.kulhad.manager.ui.theme.OverlayWhite07
import com.kulhad.manager.ui.theme.Success
import com.kulhad.manager.ui.theme.SurfaceCard
import com.kulhad.manager.ui.theme.TextPrimary
import com.kulhad.manager.ui.theme.TextSecondary
import com.kulhad.manager.ui.theme.WarningAmber
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

// ── Filter enum ───────────────────────────────────────────────────────────────

enum class RegisterFilter { ALL, PIECE, SALARY }

// ── UI state ──────────────────────────────────────────────────────────────────

data class RegisterUiState(
    val monthLabel: String = "",
    val rows: List<WorkerMonthAttendance> = emptyList(),
    val totalWorkers: Int = 0,
    val totalPresentDays: Int = 0,
    val totalAbsentDays: Int = 0,
    val avgRate: Int = 0
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class AttendanceRegisterViewModel @Inject constructor(
    private val repository: WorkerRepository
) : ViewModel() {

    // Anchor = first-of-month epoch millis for the currently displayed month
    private val _registerMonth = MutableStateFlow(DateUtils.startOfMonth(System.currentTimeMillis()))
    val registerMonth: StateFlow<Long> = _registerMonth.asStateFlow()

    private val _filter = MutableStateFlow(RegisterFilter.ALL)
    val filter: StateFlow<RegisterFilter> = _filter.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<RegisterUiState> = combine(
        _registerMonth.flatMapLatest { anchor ->
            val from = DateUtils.startOfMonth(anchor)
            val to   = DateUtils.endOfMonth(anchor)
            repository.observeMonthlyAttendanceReport(from, to)
        },
        _filter
    ) { allRows, filter ->
        val filtered = when (filter) {
            RegisterFilter.ALL    -> allRows
            RegisterFilter.PIECE  -> allRows.filter { it.worker.currentType == WorkerType.PIECE }
            RegisterFilter.SALARY -> allRows.filter { it.worker.currentType == WorkerType.SALARY }
        }

        // Sort: rate% desc → present days desc → name asc
        val sorted = filtered.sortedWith(
            compareByDescending<WorkerMonthAttendance> { it.rate }
                .thenByDescending { it.presentDays }
                .thenBy { it.worker.name }
        )

        val totalPresent = filtered.sumOf { it.presentDays }
        val totalAbsent  = filtered.sumOf { it.absentDays }
        val totalRecorded = totalPresent + totalAbsent
        val avgRate = if (totalRecorded == 0) 0 else (totalPresent * 100) / totalRecorded

        RegisterUiState(
            monthLabel       = DateUtils.formatMonth(_registerMonth.value),
            rows             = sorted,
            totalWorkers     = filtered.size,
            totalPresentDays = totalPresent,
            totalAbsentDays  = totalAbsent,
            avgRate          = avgRate
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        RegisterUiState()
    )

    fun prevMonth() {
        _registerMonth.value = DateUtils.addMonths(_registerMonth.value, -1)
    }

    fun nextMonth() {
        val next = DateUtils.addMonths(_registerMonth.value, 1)
        // Do not allow navigating past the current month
        if (next <= DateUtils.startOfMonth(System.currentTimeMillis())) {
            _registerMonth.value = next
        }
    }

    fun setFilter(f: RegisterFilter) {
        _filter.value = f
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun AttendanceHistoryScreen(
    onBack: () -> Unit,
    onWorkerHistory: (Long) -> Unit,
    viewModel: AttendanceRegisterViewModel = hiltViewModel()
) {
    val state  by viewModel.uiState.collectAsStateWithLifecycle()
    val month  by viewModel.registerMonth.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()

    // Is the currently displayed month the current calendar month? If so, disable Next.
    val isCurrentMonth = DateUtils.startOfMonth(month) ==
        DateUtils.startOfMonth(System.currentTimeMillis())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
    ) {
        KulhadTopBar(title = "Attendance Register", onBack = onBack)

        LazyColumn(
            contentPadding      = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            // ── Month navigation ──────────────────────────────────────────────
            item {
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceCard)
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Previous month
                    Box(
                        modifier         = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable { viewModel.prevMonth() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.ChevronLeft,
                            contentDescription = "Previous month",
                            tint     = TextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Text(
                        text       = state.monthLabel,
                        color      = TextPrimary,
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.W600
                    )

                    // Next month — greyed out when on current month
                    Box(
                        modifier         = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable(enabled = !isCurrentMonth) { viewModel.nextMonth() }
                            .alpha(if (isCurrentMonth) 0.35f else 1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.ChevronRight,
                            contentDescription = "Next month",
                            tint     = TextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // ── KPI strip ────────────────────────────────────────────────────
            item {
                KpiStrip(
                    items = listOf(
                        Triple(state.totalWorkers.toString(),     "Workers",  InfoBlue),
                        Triple(state.totalPresentDays.toString(), "Present",  Success),
                        Triple(state.totalAbsentDays.toString(),  "Absent",   ErrorRed),
                        Triple("${state.avgRate}%",               "Avg Rate", WarningAmber)
                    )
                )
            }

            // ── Type filter ──────────────────────────────────────────────────
            item {
                SegmentedControl(
                    options  = listOf("All", "Piece", "Salary"),
                    selected = when (filter) {
                        RegisterFilter.ALL    -> "All"
                        RegisterFilter.PIECE  -> "Piece"
                        RegisterFilter.SALARY -> "Salary"
                    },
                    onSelect = {
                        viewModel.setFilter(
                            when (it) {
                                "Piece"  -> RegisterFilter.PIECE
                                "Salary" -> RegisterFilter.SALARY
                                else     -> RegisterFilter.ALL
                            }
                        )
                    }
                )
            }

            // ── Worker rows ──────────────────────────────────────────────────
            if (state.rows.isEmpty()) {
                item {
                    Box(
                        modifier         = Modifier
                            .fillMaxWidth()
                            .padding(top = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Outlined.PersonOutline,
                                contentDescription = null,
                                tint     = TextSecondary,
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text     = "No attendance records for this month",
                                color    = TextSecondary,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            } else {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(SurfaceCard)
                            .padding(horizontal = 12.dp)
                    ) {
                        state.rows.forEachIndexed { idx, entry ->
                            RegisterWorkerRow(
                                entry     = entry,
                                onClick   = { onWorkerHistory(entry.worker.id) }
                            )
                            if (idx < state.rows.lastIndex) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(0.5.dp)
                                        .background(OverlayWhite07)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Register worker row ───────────────────────────────────────────────────────

@Composable
private fun RegisterWorkerRow(
    entry: WorkerMonthAttendance,
    onClick: () -> Unit
) {
    val worker   = entry.worker
    val isActive = worker.isActive

    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .alpha(if (isActive) 1f else 0.65f)
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        WorkerAvatar(name = worker.name, size = 36.dp, fontSize = 11)

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = worker.name,
                color      = if (isActive) TextPrimary else TextSecondary,
                fontSize   = 14.sp,
                fontWeight = if (isActive) FontWeight.W500 else FontWeight.W400
            )
            val typeLabel = if (worker.currentType == WorkerType.PIECE) "Piece" else "Salary"
            Text(text = typeLabel, color = TextSecondary, fontSize = 12.sp)
        }

        // Active / Inactive badge
        if (isActive) {
            StatusBadge("Active", BadgeType.SUCCESS)
        } else {
            StatusBadge("Inactive", BadgeType.WARNING)
        }

        // Attendance stats column
        Column(horizontalAlignment = Alignment.End) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text  = "P: ${entry.presentDays}",
                    color = Success,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.W500
                )
                Text(
                    text  = "A: ${entry.absentDays}",
                    color = ErrorRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.W500
                )
            }
            Text(
                text  = "${entry.rate}%",
                color = when {
                    entry.rate >= 90 -> Success
                    entry.rate >= 70 -> WarningAmber
                    else             -> ErrorRed
                },
                fontSize   = 13.sp,
                fontWeight = FontWeight.W600
            )
        }
    }
}

// ── Legacy composables kept for compilation (no longer used in this screen) ───
// AttendanceEditDialog and WorkerFilterDropdown are removed; the register
// screen is read-only — edits happen via WorkerHistoryScreen.
