package com.kulhad.manager.ui.screens.workers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.kulhad.manager.ui.charts.SimpleLineChart
import com.kulhad.manager.ui.components.BadgeType
import com.kulhad.manager.ui.components.KulhadButton
import com.kulhad.manager.ui.components.KulhadButtonStyle
import com.kulhad.manager.ui.components.KulhadTopBar
import com.kulhad.manager.ui.components.StatusBadge
import com.kulhad.manager.ui.components.WorkerAvatar
import com.kulhad.manager.ui.theme.BgDeep
import com.kulhad.manager.ui.theme.ErrorRed
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

    Column(modifier = Modifier.fillMaxSize().background(BgDeep)) {
        KulhadTopBar(
            title = "Attendance",
            subtitle = DateUtils.formatDay(System.currentTimeMillis()),
            onBack = onBack
        )
        LazyColumn(
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceCard)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("ATTENDANCE TREND • LAST 7 DAYS", color = TextSecondary, fontSize = 9.sp, letterSpacing = 0.5.sp)
                    SimpleLineChart(
                        values = trend.map { it.second.toFloat() },
                        chartHeight = 60.dp,
                        lineColor = Success
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatPill("$present present", Success, Modifier.weight(1f))
                    StatPill("$absent absent", ErrorRed, Modifier.weight(1f))
                    StatPill("$rate% rate", WarningAmber, Modifier.weight(1f))
                }
            }
            items(workers, key = { it.id }) { w ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceCard)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
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
                    WorkerAvatar(name = w.name, size = 28.dp, fontSize = 9)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = w.name, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.W500)
                        val typeLabel = if (w.currentType == WorkerType.PIECE) "Piece" else "Salary"
                        Text(text = typeLabel, color = TextSecondary, fontSize = 10.sp)
                    }
                    val isP = checked[w.id] == true
                    StatusBadge(if (isP) "Present" else "Absent", if (isP) BadgeType.SUCCESS else BadgeType.ERROR)
                }
            }
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

@Composable
private fun StatPill(text: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceCard)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = color, fontSize = 11.sp, fontWeight = FontWeight.W500)
    }
}
