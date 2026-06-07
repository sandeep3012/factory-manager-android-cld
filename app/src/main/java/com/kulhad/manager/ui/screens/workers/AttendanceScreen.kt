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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kulhad.manager.data.local.entity.WorkerType
import com.kulhad.manager.data.util.DateUtils
import java.time.ZoneId
import com.kulhad.manager.ui.charts.SimpleLineChart
import com.kulhad.manager.ui.components.BadgeType
import com.kulhad.manager.ui.components.KpiStrip
import com.kulhad.manager.ui.components.KulhadButton
import com.kulhad.manager.ui.components.KulhadButtonStyle
import com.kulhad.manager.ui.components.KulhadTopBar
import com.kulhad.manager.ui.components.StatusBadge
import com.kulhad.manager.ui.components.WorkerAvatar
import com.kulhad.manager.ui.components.WorkingDateChip
import com.kulhad.manager.ui.preview.UiDemoData
import com.kulhad.manager.ui.theme.BgDeep
import com.kulhad.manager.ui.theme.ErrorRed
import com.kulhad.manager.ui.theme.InfoBlue
import com.kulhad.manager.ui.theme.OverlayWhite07
import com.kulhad.manager.ui.theme.Success
import com.kulhad.manager.ui.theme.SurfaceCard
import com.kulhad.manager.ui.theme.TextPrimary
import com.kulhad.manager.ui.theme.TextSecondary
import com.kulhad.manager.ui.theme.TextTertiary
import com.kulhad.manager.ui.theme.WarningAmber

@Composable
fun AttendanceScreen(
    onBack: () -> Unit,
    onHistory: () -> Unit,
    viewModel: WorkerViewModel = hiltViewModel()
) {
    val state       by viewModel.attendanceScreenState.collectAsStateWithLifecycle()
    val trend       by viewModel.attendanceTrend.collectAsStateWithLifecycle()
    val workingDate by viewModel.workingDate.collectAsStateWithLifecycle()

    // Checkbox map — holds ONLY active workers' editable state.
    // Inactive workers are never placed here; they are read-only from state.
    val checked = remember { mutableStateMapOf<Long, Boolean>() }

    // Collapsed/expanded state for the inactive section (default: expanded)
    var inactiveExpanded by remember { mutableStateOf(true) }

    // Sync checkboxes from the saved DB map whenever the active worker list or the
    // attendance map changes. Always overwrites — stale checkbox state must never
    // persist across date changes (flatMapLatest in the VM handles map resets).
    LaunchedEffect(state.activeWorkers, state.attendanceMap) {
        state.activeWorkers.forEach { w ->
            checked[w.id] = state.attendanceMap[w.id] ?: false
        }
        // Drop entries for workers that are no longer in the active list
        checked.keys.retainAll(state.activeWorkers.map { it.id }.toSet())
    }

    // ── KPI: computed live from checkbox state + read-only inactive rows ───────
    // This makes the KPI strip reflect exactly what is visible on screen:
    //   - Active workers: from the editable checked map (live, not the saved DB count)
    //   - Inactive workers (past dates): from their fixed saved DB state
    val presentCount = checked.values.count { it } +
        state.inactiveAttendanceRows.count { it.isPresent }
    val absentCount  = checked.values.count { !it } +
        state.inactiveAttendanceRows.count { !it.isPresent }
    val totalVisible = state.activeWorkers.size + state.inactiveAttendanceRows.size
    val rate         = if (totalVisible == 0) 0 else (presentCount * 100) / totalVisible

    // Demo mode: shown when no active workers exist and the demo flag is set
    val useDemo     = UiDemoData.SHOW_DEMO && state.activeWorkers.isEmpty()
    val dispPresent = if (useDemo) UiDemoData.workerPresent else presentCount
    val dispAbsent  = if (useDemo) UiDemoData.workerAbsent  else absentCount
    val dispRate    = if (useDemo) 86 else rate
    val dispTrend   = if (useDemo) listOf(22f, 25f, 24f, 26f, 23f, 24f, 24f)
                      else trend.map { it.second.toFloat() }

    // Section headers are only shown on past dates when inactive rows are present
    val showSections = !state.isCurrentDate && state.inactiveAttendanceRows.isNotEmpty()

    Column(modifier = Modifier.fillMaxSize().background(BgDeep)) {
        KulhadTopBar(
            title    = "Attendance",
            subtitle = DateUtils.formatDay(
                workingDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            ),
            onBack   = onBack,
            actions  = {
                IconButton(onClick = onHistory) {
                    Icon(
                        imageVector        = Icons.Outlined.History,
                        contentDescription = "Attendance History",
                        tint               = TextPrimary
                    )
                }
            }
        )

        LazyColumn(
            contentPadding      = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            // ── Working date chip ─────────────────────────────────────────────
            item {
                WorkingDateChip(
                    selectedDate   = workingDate,
                    onDateSelected = { viewModel.setWorkingDate(it) }
                )
            }

            // ── Trend chart ───────────────────────────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceCard)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "ATTENDANCE TREND • LAST 7 DAYS",
                        color = TextSecondary, fontSize = 12.sp, letterSpacing = 0.5.sp
                    )
                    SimpleLineChart(
                        values      = dispTrend.ifEmpty { listOf(0f) },
                        chartHeight = 72.dp,
                        lineColor   = Success
                    )
                }
            }

            // ── KPI strip ─────────────────────────────────────────────────────
            item {
                KpiStrip(
                    items = listOf(
                        Triple(dispPresent.toString(), "Present", Success),
                        Triple(dispAbsent.toString(),  "Absent",  ErrorRed),
                        Triple("$dispRate%",           "Rate",    InfoBlue)
                    )
                )
            }

            // ── Demo rows ─────────────────────────────────────────────────────
            if (useDemo) {
                items(UiDemoData.workers) { w ->
                    val demoChecked = remember { mutableStateOf(w.isPresent) }
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier              = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Checkbox(
                                checked         = demoChecked.value,
                                onCheckedChange = { demoChecked.value = it },
                                colors          = CheckboxDefaults.colors(
                                    checkedColor   = Success,
                                    uncheckedColor = TextSecondary,
                                    checkmarkColor = TextPrimary
                                )
                            )
                            WorkerAvatar(name = w.name, size = 34.dp, fontSize = 11)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(w.name, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.W500)
                                Text(w.type, color = TextSecondary, fontSize = 12.sp)
                            }
                            val isP = demoChecked.value
                            StatusBadge(if (isP) "Present" else "Absent", if (isP) BadgeType.SUCCESS else BadgeType.ERROR)
                        }
                        if (w != UiDemoData.workers.last()) {
                            Box(Modifier.fillMaxWidth().height(0.5.dp).background(OverlayWhite07))
                        }
                    }
                }
            }

            // ── Active workers section ────────────────────────────────────────
            if (!useDemo) {

                // "ACTIVE WORKERS" label — only when the inactive section is also visible
                if (showSections) {
                    item {
                        Text(
                            text          = "ACTIVE WORKERS",
                            color         = TextTertiary,
                            fontSize      = 11.sp,
                            fontWeight    = FontWeight.W500,
                            letterSpacing = 0.7.sp,
                            modifier      = Modifier.padding(start = 4.dp, bottom = 2.dp)
                        )
                    }
                }

                // Active workers card (all rows in one SurfaceCard)
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(SurfaceCard)
                            .padding(horizontal = 12.dp)
                    ) {
                        if (state.activeWorkers.isEmpty()) {
                            Box(
                                modifier         = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text     = "No active workers",
                                    color    = TextSecondary,
                                    fontSize = 14.sp
                                )
                            }
                        } else {
                            state.activeWorkers.forEachIndexed { idx, w ->
                                Row(
                                    modifier              = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp),
                                    verticalAlignment     = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Checkbox(
                                        checked         = checked[w.id] ?: false,
                                        onCheckedChange = { checked[w.id] = it },
                                        colors          = CheckboxDefaults.colors(
                                            checkedColor   = Success,
                                            uncheckedColor = TextSecondary,
                                            checkmarkColor = TextPrimary
                                        )
                                    )
                                    WorkerAvatar(name = w.name, size = 34.dp, fontSize = 11)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text       = w.name,
                                            color      = TextPrimary,
                                            fontSize   = 14.sp,
                                            fontWeight = FontWeight.W500
                                        )
                                        val typeLabel = if (w.currentType == WorkerType.PIECE) "Piece" else "Salary"
                                        Text(typeLabel, color = TextSecondary, fontSize = 12.sp)
                                    }
                                    val isP = checked[w.id] == true
                                    StatusBadge(
                                        if (isP) "Present" else "Absent",
                                        if (isP) BadgeType.SUCCESS else BadgeType.ERROR
                                    )
                                }
                                if (idx < state.activeWorkers.lastIndex) {
                                    Box(Modifier.fillMaxWidth().height(0.5.dp).background(OverlayWhite07))
                                }
                            }
                        }
                    }
                }

                // ── Inactive workers section (past dates only) ────────────────
                // Rule 2: inactive workers with records appear only on past dates.
                // Rule 3: rows are read-only (disabled checkbox, INACTIVE badge, dimmed).
                // Rule 5: collapsible — default expanded.
                if (showSections) {

                    // Collapsible section header
                    item {
                        Row(
                            modifier              = Modifier
                                .fillMaxWidth()
                                .clickable { inactiveExpanded = !inactiveExpanded }
                                .padding(
                                    start   = 4.dp,
                                    end     = 4.dp,
                                    top     = 6.dp,
                                    bottom  = 2.dp
                                ),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text          = "INACTIVE WORKERS (${state.inactiveAttendanceRows.size})",
                                color         = TextTertiary,
                                fontSize      = 11.sp,
                                fontWeight    = FontWeight.W500,
                                letterSpacing = 0.7.sp
                            )
                            Icon(
                                imageVector        = if (inactiveExpanded) Icons.Filled.KeyboardArrowUp
                                                     else Icons.Filled.KeyboardArrowDown,
                                contentDescription = if (inactiveExpanded) "Collapse" else "Expand",
                                tint               = TextTertiary,
                                modifier           = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Inactive workers card — shown only when expanded
                    if (inactiveExpanded) {
                        item {
                            // alpha applied first so it dims both background and content
                            Column(
                                modifier = Modifier
                                    .alpha(0.65f)
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(SurfaceCard)
                                    .padding(horizontal = 12.dp)
                            ) {
                                state.inactiveAttendanceRows.forEachIndexed { idx, entry ->
                                    Row(
                                        modifier              = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 10.dp),
                                        verticalAlignment     = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Disabled checkbox — shows saved P/A state, non-interactive
                                        Checkbox(
                                            checked         = entry.isPresent,
                                            onCheckedChange = null,
                                            colors          = CheckboxDefaults.colors(
                                                checkedColor           = Success,
                                                uncheckedColor         = TextSecondary,
                                                checkmarkColor         = TextPrimary,
                                                disabledCheckedColor   = Success,
                                                disabledUncheckedColor = TextSecondary
                                            )
                                        )
                                        WorkerAvatar(
                                            name     = entry.worker.name,
                                            size     = 34.dp,
                                            fontSize = 11
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text       = entry.worker.name,
                                                color      = TextSecondary,
                                                fontSize   = 14.sp,
                                                fontWeight = FontWeight.W500
                                            )
                                            val typeLabel = if (entry.worker.currentType == WorkerType.PIECE)
                                                "Piece" else "Salary"
                                            Text(typeLabel, color = TextTertiary, fontSize = 12.sp)
                                        }
                                        StatusBadge("Inactive", BadgeType.WARNING)
                                    }
                                    if (idx < state.inactiveAttendanceRows.lastIndex) {
                                        Box(Modifier.fillMaxWidth().height(0.5.dp).background(OverlayWhite07))
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Save button ───────────────────────────────────────────────
                // Rule 6: only active workers' checkbox state is saved.
                // checked is seeded exclusively with active worker IDs (via LaunchedEffect),
                // so filtering to activeIds is a belt-and-suspenders safety net.
                item {
                    KulhadButton(
                        text    = "Save Attendance",
                        style   = KulhadButtonStyle.SUCCESS,
                        onClick = {
                            val activeIds = state.activeWorkers.map { it.id }.toSet()
                            viewModel.saveAttendance(
                                checked.filterKeys { it in activeIds }.toMap()
                            ) { onBack() }
                        }
                    )
                }
            }
        }
    }
}
