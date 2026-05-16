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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kulhad.manager.data.local.entity.WorkerType
import com.kulhad.manager.data.util.Money
import com.kulhad.manager.domain.model.WorkerWithAttendance
import com.kulhad.manager.ui.components.BadgeType
import com.kulhad.manager.ui.components.KpiStrip
import com.kulhad.manager.ui.components.KulhadTopBar
import com.kulhad.manager.ui.components.SegmentedControl
import com.kulhad.manager.ui.components.StatusBadge
import com.kulhad.manager.ui.components.WorkerAvatar
import com.kulhad.manager.ui.preview.UiDemoData
import com.kulhad.manager.ui.theme.BgDeep
import com.kulhad.manager.ui.theme.ErrorRed
import com.kulhad.manager.ui.theme.InfoBlue
import com.kulhad.manager.ui.theme.OverlayWhite07
import com.kulhad.manager.ui.theme.PrimaryBlue
import com.kulhad.manager.ui.theme.Success
import com.kulhad.manager.ui.theme.TextPrimary
import com.kulhad.manager.ui.theme.TextSecondary

@Composable
fun WorkerListScreen(
    onAddWorker: () -> Unit,
    onEditWorker: (Long) -> Unit,
    onTypeHistory: (Long) -> Unit,
    onAttendance: () -> Unit,
    onAdvanceEntry: () -> Unit,
    viewModel: WorkerViewModel = hiltViewModel()
) {
    val data by viewModel.listData.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()

    // Demo overlay when DB is empty
    val useDemo = UiDemoData.SHOW_DEMO && data.workers.isEmpty()
    val dispTotal   = if (useDemo) UiDemoData.workerTotal   else data.totalCount
    val dispPresent = if (useDemo) UiDemoData.workerPresent else data.presentCount
    val dispAbsent  = if (useDemo) UiDemoData.workerAbsent  else data.absentCount

    Column(modifier = Modifier.fillMaxSize().background(BgDeep)) {
        KulhadTopBar(
            title = "Workers",
            subtitle = "$dispTotal registered",
            actions = {
                IconButton(onClick = onAttendance) {
                    Icon(Icons.Outlined.History, contentDescription = "Attendance", tint = TextPrimary)
                }
                IconButton(onClick = onAdvanceEntry) {
                    Icon(Icons.Outlined.Savings, contentDescription = "Advance", tint = TextPrimary)
                }
                IconButton(onClick = onAddWorker) {
                    Icon(Icons.Filled.Add, contentDescription = "Add", tint = PrimaryBlue)
                }
            }
        )

        // Compact KPI strip (matches HTML screen 3 — 3-col compact stats)
        val rate = if (dispTotal > 0) (dispPresent * 100) / dispTotal else 0
        KpiStrip(
            items = listOf(
                Triple(dispTotal.toString(),   "Total",   TextPrimary),
                Triple(dispPresent.toString(), "Present", Success),
                Triple(dispAbsent.toString(),  "Absent",  ErrorRed),
                Triple("$rate%",               "Rate",    InfoBlue)
            ),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )

        SegmentedControl(
            options = listOf("All", "Piece", "Salary"),
            selected = when (filter) {
                WorkerFilter.ALL    -> "All"
                WorkerFilter.PIECE  -> "Piece"
                WorkerFilter.SALARY -> "Salary"
            },
            onSelect = {
                viewModel.setFilter(
                    when (it) {
                        "Piece"  -> WorkerFilter.PIECE
                        "Salary" -> WorkerFilter.SALARY
                        else     -> WorkerFilter.ALL
                    }
                )
            },
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
        )

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            if (useDemo) {
                // ── Demo rows ──────────────────────────────────────────────
                items(UiDemoData.workers) { w ->
                    DemoWorkerRow(
                        name = w.name,
                        detail = "${w.type} · ${w.detail}",
                        isPresent = w.isPresent,
                        isLast = w == UiDemoData.workers.last()
                    )
                }
            } else if (data.workers.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                        contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Outlined.PersonOutline, contentDescription = null,
                                tint = TextSecondary, modifier = Modifier.size(48.dp))
                            Text("No workers added yet", color = TextSecondary, fontSize = 14.sp)
                        }
                    }
                }
            } else {
                items(data.workers, key = { it.worker.id }) { item ->
                    val isLast = item == data.workers.last()
                    WorkerRow(
                        item = item,
                        onClick = { onEditWorker(item.worker.id) },
                        onHistory = { onTypeHistory(item.worker.id) },
                        isLast = isLast
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkerRow(
    item: WorkerWithAttendance,
    onClick: () -> Unit,
    onHistory: () -> Unit,
    isLast: Boolean = false
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            WorkerAvatar(name = item.worker.name, size = 38.dp, fontSize = 12)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.worker.name, color = TextPrimary,
                    fontSize = 14.sp, fontWeight = FontWeight.W500)
                val typeStr = if (item.worker.currentType == WorkerType.PIECE)
                    "Piece · ${item.worker.phone}" else
                    "Salary · ${Money.formatRupees(item.worker.dailyRate)}/day"
                Text(text = typeStr, color = TextSecondary, fontSize = 12.sp)
            }
            when (item.isPresentToday) {
                true  -> StatusBadge("Present", BadgeType.SUCCESS)
                false -> StatusBadge("Absent",  BadgeType.ERROR)
                null  -> StatusBadge("—",       BadgeType.INFO)
            }
            Box(
                modifier = Modifier.size(31.dp).clip(CircleShape).clickable { onHistory() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.History, contentDescription = "History",
                    tint = TextSecondary, modifier = Modifier.size(19.dp))
            }
        }
        if (!isLast) {
            Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(OverlayWhite07))
        }
    }
}

@Composable
private fun DemoWorkerRow(name: String, detail: String, isPresent: Boolean, isLast: Boolean = false) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            WorkerAvatar(name = name, size = 38.dp, fontSize = 12)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = name, color = TextPrimary,
                    fontSize = 14.sp, fontWeight = FontWeight.W500)
                Text(text = detail, color = TextSecondary, fontSize = 12.sp)
            }
            StatusBadge(if (isPresent) "Present" else "Absent",
                if (isPresent) BadgeType.SUCCESS else BadgeType.ERROR)
            Box(modifier = Modifier.size(31.dp), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.History, contentDescription = null,
                    tint = TextSecondary, modifier = Modifier.size(19.dp))
            }
        }
        if (!isLast) {
            Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(OverlayWhite07))
        }
    }
}
