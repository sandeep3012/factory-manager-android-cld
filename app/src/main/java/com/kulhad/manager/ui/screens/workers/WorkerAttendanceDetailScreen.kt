package com.kulhad.manager.ui.screens.workers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.kulhad.manager.data.local.entity.WorkerType
import com.kulhad.manager.data.repository.WorkerRepository
import com.kulhad.manager.data.util.DateUtils
import com.kulhad.manager.domain.model.Worker
import com.kulhad.manager.ui.components.BadgeType
import com.kulhad.manager.ui.components.KpiStrip
import com.kulhad.manager.ui.components.KulhadTopBar
import com.kulhad.manager.ui.components.StatusBadge
import com.kulhad.manager.ui.components.WorkerAvatar
import com.kulhad.manager.ui.theme.BgDeep
import com.kulhad.manager.ui.theme.ErrorRed
import com.kulhad.manager.ui.theme.Success
import com.kulhad.manager.ui.theme.SuccessDark
import com.kulhad.manager.ui.theme.SurfaceCard
import com.kulhad.manager.ui.theme.TextPrimary
import com.kulhad.manager.ui.theme.TextSecondary
import com.kulhad.manager.ui.theme.TextTertiary
import com.kulhad.manager.ui.theme.WarningAmber
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Calendar
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

// ── UI state ──────────────────────────────────────────────────────────────────

/**
 * All data needed to render the monthly attendance detail screen.
 *
 * [dailyStatus] maps day-of-month (1..daysInMonth) to present/absent.
 * Days absent from the map have no attendance record for that day.
 * [firstDayOffset] is 0-based Monday-first (Mon=0, Tue=1, …, Sun=6) so the
 * calendar grid can correctly align the first date under the right weekday column.
 */
data class AttendanceDetailState(
    val monthLabel: String = "",
    val presentDays: Int = 0,
    val absentDays: Int = 0,
    val rate: Int = 0,
    val daysInMonth: Int = 30,
    val firstDayOffset: Int = 0,
    val dailyStatus: Map<Int, Boolean> = emptyMap()  // day-of-month → isPresent
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class WorkerAttendanceDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: WorkerRepository
) : ViewModel() {

    private val workerId: Long = checkNotNull(savedStateHandle["workerId"])

    // Start at the month the register was showing when user tapped the row
    private val _month = MutableStateFlow(
        DateUtils.startOfMonth(checkNotNull<Long>(savedStateHandle["month"]))
    )
    val month: StateFlow<Long> = _month.asStateFlow()

    val worker: StateFlow<Worker?> = repository.observeWorker(workerId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val detailState: StateFlow<AttendanceDetailState> =
        _month.flatMapLatest { anchor ->
            val from = DateUtils.startOfMonth(anchor)
            val to   = DateUtils.endOfMonth(anchor)
            repository.observeWorkerAttendanceInRange(workerId, from, to)
                .map { records ->
                    // Map day-of-month → isPresent
                    val dailyStatus = records.associate { rec ->
                        val cal = Calendar.getInstance().apply { timeInMillis = rec.date }
                        cal.get(Calendar.DAY_OF_MONTH) to rec.isPresent
                    }

                    // Calendar layout metadata
                    val cal = Calendar.getInstance().apply { timeInMillis = anchor }
                    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                    cal.set(Calendar.DAY_OF_MONTH, 1)
                    // Calendar.DAY_OF_WEEK: Sun=1 Mon=2 … Sat=7
                    // Convert to Mon-first offset: Mon=0 … Sun=6
                    val firstDayOffset = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7

                    val presentDays  = records.count { it.isPresent }
                    val absentDays   = records.count { !it.isPresent }
                    val totalRecorded = presentDays + absentDays
                    val rate = if (totalRecorded == 0) 0 else (presentDays * 100) / totalRecorded

                    AttendanceDetailState(
                        monthLabel     = DateUtils.formatMonth(anchor),
                        presentDays    = presentDays,
                        absentDays     = absentDays,
                        rate           = rate,
                        daysInMonth    = daysInMonth,
                        firstDayOffset = firstDayOffset,
                        dailyStatus    = dailyStatus
                    )
                }
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            AttendanceDetailState()
        )

    fun prevMonth() {
        _month.value = DateUtils.addMonths(_month.value, -1)
    }

    fun nextMonth() {
        val next = DateUtils.addMonths(_month.value, 1)
        if (next <= DateUtils.startOfMonth(System.currentTimeMillis())) {
            _month.value = next
        }
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun WorkerAttendanceDetailScreen(
    onBack: () -> Unit,
    viewModel: WorkerAttendanceDetailViewModel = hiltViewModel()
) {
    val worker      by viewModel.worker.collectAsStateWithLifecycle()
    val state       by viewModel.detailState.collectAsStateWithLifecycle()
    val month       by viewModel.month.collectAsStateWithLifecycle()

    val isCurrentMonth = DateUtils.startOfMonth(month) ==
        DateUtils.startOfMonth(System.currentTimeMillis())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
    ) {
        KulhadTopBar(title = "Attendance Detail", onBack = onBack)

        LazyColumn(
            contentPadding      = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            // ── Worker header card ────────────────────────────────────────────
            worker?.let { w ->
                item {
                    Row(
                        modifier              = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(SurfaceCard)
                            .padding(14.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        WorkerAvatar(name = w.name, size = 48.dp, fontSize = 15)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text       = w.name,
                                color      = TextPrimary,
                                fontSize   = 17.sp,
                                fontWeight = FontWeight.W700
                            )
                            val typeLabel = if (w.currentType == WorkerType.PIECE)
                                "Piece Worker" else "Salary Worker"
                            Text(text = typeLabel, color = TextSecondary, fontSize = 13.sp)
                        }
                        StatusBadge(
                            text = if (w.isActive) "Active" else "Inactive",
                            type = if (w.isActive) BadgeType.SUCCESS else BadgeType.WARNING
                        )
                    }
                }
            }

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

            // ── Summary KPI strip ─────────────────────────────────────────────
            item {
                KpiStrip(
                    items = listOf(
                        Triple(state.presentDays.toString(), "Present Days", Success),
                        Triple(state.absentDays.toString(),  "Absent Days",  ErrorRed),
                        Triple("${state.rate}%",             "Avg Attend.",  WarningAmber)
                    )
                )
            }

            // ── Calendar card ─────────────────────────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceCard)
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Day-of-week header (Monday-first)
                    val dayHeaders = listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")
                    Row(modifier = Modifier.fillMaxWidth()) {
                        dayHeaders.forEach { label ->
                            Box(
                                modifier         = Modifier
                                    .weight(1f)
                                    .height(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text       = label,
                                    color      = TextSecondary,
                                    fontSize   = 11.sp,
                                    fontWeight = FontWeight.W600
                                )
                            }
                        }
                    }

                    // Calendar grid
                    AttendanceCalendarGrid(state = state)
                }
            }

            // ── Legend ────────────────────────────────────────────────────────
            item {
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceCard)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    LegendItem(color = SuccessDark, label = "Present")
                    LegendItem(color = ErrorRed.copy(alpha = 0.25f), label = "Absent")
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("—", color = TextTertiary, fontSize = 14.sp, fontWeight = FontWeight.W600)
                        Text("No record", color = TextSecondary, fontSize = 12.sp)
                    }
                }
            }

            // Bottom breathing room
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

// ── Calendar grid ─────────────────────────────────────────────────────────────

@Composable
private fun AttendanceCalendarGrid(state: AttendanceDetailState) {
    // Build a flat list of cells: null = empty padding cell, Int = day-of-month
    val totalCells = state.firstDayOffset + state.daysInMonth
    val paddedTotal = if (totalCells % 7 == 0) totalCells else totalCells + (7 - totalCells % 7)

    val cells: List<Int?> = buildList {
        repeat(state.firstDayOffset) { add(null) }
        for (day in 1..state.daysInMonth) add(day)
        val trailing = paddedTotal - size
        repeat(trailing) { add(null) }
    }

    // Render in rows of 7
    val rows = cells.chunked(7)
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        rows.forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    Box(
                        modifier         = Modifier
                            .weight(1f)
                            .height(44.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (day != null) {
                            val status: Boolean? = state.dailyStatus[day]
                            CalendarDayCell(day = day, status = status)
                        }
                    }
                }
            }
        }
    }
}

// ── Calendar day cell ─────────────────────────────────────────────────────────
//
//  status = true   → green circle, white day number   (Present)
//  status = false  → red-tinted circle, red day number (Absent)
//  status = null   → no circle, dim grey day number   (No record)
//

@Composable
private fun CalendarDayCell(day: Int, status: Boolean?) {
    val circleBg = when (status) {
        true  -> SuccessDark
        false -> ErrorRed.copy(alpha = 0.22f)
        null  -> Color.Transparent
    }
    val textColor = when (status) {
        true  -> Color.White
        false -> ErrorRed
        null  -> TextTertiary
    }
    val fontWeight = if (status != null) FontWeight.W700 else FontWeight.W400

    Box(
        modifier         = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(circleBg),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text       = day.toString(),
            color      = textColor,
            fontSize   = 13.sp,
            fontWeight = fontWeight,
            textAlign  = TextAlign.Center
        )
    }
}

// ── Legend item ───────────────────────────────────────────────────────────────

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(text = label, color = TextSecondary, fontSize = 12.sp)
    }
}
