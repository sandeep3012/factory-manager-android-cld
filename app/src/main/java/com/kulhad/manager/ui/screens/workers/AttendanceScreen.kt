package com.kulhad.manager.ui.screens.workers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.kulhad.manager.ui.theme.WarningAmber

@Composable
fun AttendanceScreen(
    onBack: () -> Unit,
    viewModel: WorkerViewModel = hiltViewModel()
) {
    val workers by viewModel.activeWorkers.collectAsStateWithLifecycle()
    val present by viewModel.attendancePresentToday.collectAsStateWithLifecycle()
    val absent by viewModel.attendanceAbsentToday.collectAsStateWithLifecycle()
    val trend by viewModel.attendanceTrend.collectAsStateWithLifecycle()
    val saved by viewModel.attendanceTodayMap.collectAsStateWithLifecycle()
    val workingDate by viewModel.workingDate.collectAsStateWithLifecycle()

    val checked = remember { mutableStateMapOf<Long, Boolean>() }

    LaunchedEffect(workers, saved) {
        workers.forEach { w ->
            if (!checked.containsKey(w.id)) {
                checked[w.id] = saved[w.id] ?: false
            }
        }
    }

    val total = workers.size
    val rate = if (total == 0) 0 else (present * 100) / total

    // Demo data: use when workers list is empty
    val useDemo = UiDemoData.SHOW_DEMO && workers.isEmpty()
    val dispPresent = if (useDemo) UiDemoData.workerPresent else present
    val dispAbsent  = if (useDemo) UiDemoData.workerAbsent  else absent
    val dispRate    = if (useDemo) 86 else rate
    val dispTrend   = if (useDemo) listOf(22f, 25f, 24f, 26f, 23f, 24f, 24f)
                      else trend.map { it.second.toFloat() }

    Column(modifier = Modifier.fillMaxSize().background(BgDeep)) {
        KulhadTopBar(
            title = "Attendance",
            subtitle = DateUtils.formatDay(
                workingDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            ),
            onBack = onBack
        )
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Working date chip — tap to change the global working date
            item {
                WorkingDateChip(
                    selectedDate = workingDate,
                    onDateSelected = { viewModel.setWorkingDate(it) }
                )
            }

            // Trend chart
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
                        values = dispTrend.ifEmpty { listOf(0f) },
                        chartHeight = 72.dp,
                        lineColor = Success
                    )
                }
            }

            // KPI strip
            item {
                KpiStrip(
                    items = listOf(
                        Triple(dispPresent.toString(), "Present", Success),
                        Triple(dispAbsent.toString(),  "Absent",  ErrorRed),
                        Triple("$dispRate%",           "Rate",    InfoBlue)
                    )
                )
            }

            // Attendance rows
            if (useDemo) {
                // Demo rows — show greyed-out interactive style
                items(UiDemoData.workers) { w ->
                    val demoChecked = remember { androidx.compose.runtime.mutableStateOf(w.isPresent) }
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Checkbox(
                                checked = demoChecked.value,
                                onCheckedChange = { demoChecked.value = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Success,
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
            } else {
                items(workers, key = { it.id }) { w ->
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Checkbox(
                                checked = checked[w.id] ?: false,
                                onCheckedChange = { checked[w.id] = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Success,
                                    uncheckedColor = TextSecondary,
                                    checkmarkColor = TextPrimary
                                )
                            )
                            WorkerAvatar(name = w.name, size = 34.dp, fontSize = 11)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(w.name, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.W500)
                                val typeLabel = if (w.currentType == WorkerType.PIECE) "Piece" else "Salary"
                                Text(typeLabel, color = TextSecondary, fontSize = 12.sp)
                            }
                            val isP = checked[w.id] == true
                            StatusBadge(if (isP) "Present" else "Absent", if (isP) BadgeType.SUCCESS else BadgeType.ERROR)
                        }
                        if (w != workers.last()) {
                            Box(Modifier.fillMaxWidth().height(0.5.dp).background(OverlayWhite07))
                        }
                    }
                }
            }

            if (!useDemo) {
                item {
                    KulhadButton(
                        text = "Save Attendance",
                        style = KulhadButtonStyle.SUCCESS,
                        onClick = {
                            viewModel.saveAttendance(checked.toMap()) { onBack() }
                        }
                    )
                }
            }
        }
    }
}
