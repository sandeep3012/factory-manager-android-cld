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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.kulhad.manager.ui.theme.BorderLine
import com.kulhad.manager.ui.theme.ErrorRed
import com.kulhad.manager.ui.theme.InfoBlue
import com.kulhad.manager.ui.theme.OverlayWhite07
import com.kulhad.manager.ui.theme.PrimaryBlue
import com.kulhad.manager.ui.theme.Success
import com.kulhad.manager.ui.theme.SurfaceCard
import com.kulhad.manager.ui.theme.TextPrimary
import com.kulhad.manager.ui.theme.TextSecondary
import com.kulhad.manager.ui.theme.TextTertiary
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

    // Anchor = start-of-month epoch millis for the currently displayed month
    private val _registerMonth = MutableStateFlow(DateUtils.startOfMonth(System.currentTimeMillis()))
    val registerMonth: StateFlow<Long> = _registerMonth.asStateFlow()

    // Type filter
    private val _filter = MutableStateFlow(RegisterFilter.ALL)
    val filter: StateFlow<RegisterFilter> = _filter.asStateFlow()

    // Active-only toggle — default OFF means show active workers only
    private val _includeInactive = MutableStateFlow(false)
    val includeInactive: StateFlow<Boolean> = _includeInactive.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<RegisterUiState> = combine(
        _registerMonth.flatMapLatest { anchor ->
            repository.observeMonthlyAttendanceReport(
                DateUtils.startOfMonth(anchor),
                DateUtils.endOfMonth(anchor)
            )
        },
        _filter,
        _includeInactive
    ) { allRows, filter, includeInactive ->

        // 1. Active / inactive gate — default: active workers only
        val afterActiveFilter = if (includeInactive) allRows
                                else allRows.filter { it.worker.isActive }

        // 2. Type filter
        val afterTypeFilter = when (filter) {
            RegisterFilter.ALL    -> afterActiveFilter
            RegisterFilter.PIECE  -> afterActiveFilter.filter { it.worker.currentType == WorkerType.PIECE }
            RegisterFilter.SALARY -> afterActiveFilter.filter { it.worker.currentType == WorkerType.SALARY }
        }

        // 3. Sort: attendance rate% ↓ → present days ↓ → name ↑
        val sorted = afterTypeFilter.sortedWith(
            compareByDescending<WorkerMonthAttendance> { it.rate }
                .thenByDescending { it.presentDays }
                .thenBy { it.worker.name }
        )

        // 4. KPI aggregates (based on type-filtered set, not just sorted)
        val totalPresent  = afterTypeFilter.sumOf { it.presentDays }
        val totalAbsent   = afterTypeFilter.sumOf { it.absentDays }
        val totalRecorded = totalPresent + totalAbsent
        val avgRate       = if (totalRecorded == 0) 0 else (totalPresent * 100) / totalRecorded

        RegisterUiState(
            monthLabel       = DateUtils.formatMonth(_registerMonth.value),
            rows             = sorted,
            totalWorkers     = afterTypeFilter.size,
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
        if (next <= DateUtils.startOfMonth(System.currentTimeMillis())) {
            _registerMonth.value = next
        }
    }

    fun setFilter(f: RegisterFilter) { _filter.value = f }

    fun setIncludeInactive(value: Boolean) { _includeInactive.value = value }
}

// ── Screen ────────────────────────────────────────────────────────────────────

/**
 * Monthly Attendance Register.
 *
 * [onWorkerDetail] receives the worker ID and the current register month (epoch millis)
 * so the detail screen can open directly to the month being viewed.
 */
@Composable
fun AttendanceHistoryScreen(
    onBack: () -> Unit,
    onWorkerDetail: (workerId: Long, monthAnchor: Long) -> Unit,
    viewModel: AttendanceRegisterViewModel = hiltViewModel()
) {
    val state          by viewModel.uiState.collectAsStateWithLifecycle()
    val month          by viewModel.registerMonth.collectAsStateWithLifecycle()
    val filter         by viewModel.filter.collectAsStateWithLifecycle()
    val includeInactive by viewModel.includeInactive.collectAsStateWithLifecycle()

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

            // ── Show inactive toggle ──────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceCard)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text       = "Show inactive workers",
                            color      = TextPrimary,
                            fontSize   = 14.sp,
                            fontWeight = FontWeight.W500
                        )
                        Text(
                            text     = if (includeInactive) "Showing all workers"
                                       else "Showing active workers only",
                            color    = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked         = includeInactive,
                        onCheckedChange = { viewModel.setIncludeInactive(it) },
                        colors          = SwitchDefaults.colors(
                            checkedThumbColor   = Color.White,
                            checkedTrackColor   = PrimaryBlue,
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = BorderLine
                        )
                    )
                }
            }

            // ── KPI strip ────────────────────────────────────────────────────
            item {
                KpiStrip(
                    items = listOf(
                        Triple(state.totalWorkers.toString(),     "Workers",      InfoBlue),
                        Triple(state.totalPresentDays.toString(), "Present Days", Success),
                        Triple(state.totalAbsentDays.toString(),  "Absent Days",  ErrorRed),
                        Triple("${state.avgRate}%",               "Avg Attend.",  WarningAmber)
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
                            .padding(top = 40.dp),
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
                                modifier = Modifier.size(44.dp)
                            )
                            Text(
                                text  = "No attendance records for this month",
                                color = TextSecondary,
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
                                entry   = entry,
                                onClick = { onWorkerDetail(entry.worker.id, month) }
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

// ── Redesigned worker row ─────────────────────────────────────────────────────
//
//  [Avatar]  Ram Kumar                            92%
//            Piece Worker  •  ACTIVE
//            Present: 24      Absent: 2
//

@Composable
private fun RegisterWorkerRow(
    entry: WorkerMonthAttendance,
    onClick: () -> Unit
) {
    val worker    = entry.worker
    val isActive  = worker.isActive
    val typeLabel = if (worker.currentType == WorkerType.PIECE) "Piece Worker" else "Salary Worker"
    val rateColor = when {
        entry.rate >= 90 -> Success
        entry.rate >= 70 -> WarningAmber
        else             -> ErrorRed
    }

    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .alpha(if (isActive) 1f else 0.65f)
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        verticalAlignment     = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Avatar — slightly larger for the card-style row
        WorkerAvatar(name = worker.name, size = 42.dp, fontSize = 13)

        // Main content block
        Column(
            modifier            = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            // Row 1: Name (left) + Rate% (right)
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    text       = worker.name,
                    color      = if (isActive) TextPrimary else TextSecondary,
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.W600
                )
                Text(
                    text       = "${entry.rate}%",
                    color      = rateColor,
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.W700
                )
            }

            // Row 2: Type label + bullet + status badge
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(text = typeLabel, color = TextSecondary, fontSize = 12.sp)
                Text(text = "•", color = TextTertiary, fontSize = 11.sp)
                StatusBadge(
                    text = if (isActive) "Active" else "Inactive",
                    type = if (isActive) BadgeType.SUCCESS else BadgeType.WARNING
                )
            }

            // Row 3: Present + Absent counts
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                Text(
                    text       = "Present: ${entry.presentDays}",
                    color      = Success,
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.W500
                )
                Text(
                    text       = "Absent: ${entry.absentDays}",
                    color      = ErrorRed,
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.W500
                )
            }
        }
    }
}
