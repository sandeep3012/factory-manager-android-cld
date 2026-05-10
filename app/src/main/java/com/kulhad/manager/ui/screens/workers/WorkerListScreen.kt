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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kulhad.manager.data.local.entity.WorkerType
import com.kulhad.manager.data.util.Money
import com.kulhad.manager.domain.model.WorkerWithAttendance
import com.kulhad.manager.ui.components.BadgeType
import com.kulhad.manager.ui.components.EmptyState
import com.kulhad.manager.ui.components.KulhadTopBar
import com.kulhad.manager.ui.components.SegmentedControl
import com.kulhad.manager.ui.components.StatCard
import com.kulhad.manager.ui.components.StatusBadge
import com.kulhad.manager.ui.components.WorkerAvatar
import com.kulhad.manager.ui.theme.BgDeep
import com.kulhad.manager.ui.theme.ErrorRed
import com.kulhad.manager.ui.theme.PrimaryBlue
import com.kulhad.manager.ui.theme.Success
import com.kulhad.manager.ui.theme.SurfaceCard
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
    ) {
        KulhadTopBar(
            title = "Workers",
            subtitle = "Tap a worker to edit",
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
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard(
                    value = data.totalCount.toString(),
                    label = "Total",
                    valueColor = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    value = data.presentCount.toString(),
                    label = "Present",
                    valueColor = Success,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    value = data.absentCount.toString(),
                    label = "Absent",
                    valueColor = ErrorRed,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        SegmentedControl(
            options = listOf("All", "Piece", "Salary"),
            selected = when (filter) {
                WorkerFilter.ALL -> "All"
                WorkerFilter.PIECE -> "Piece"
                WorkerFilter.SALARY -> "Salary"
            },
            onSelect = {
                viewModel.setFilter(
                    when (it) {
                        "Piece" -> WorkerFilter.PIECE
                        "Salary" -> WorkerFilter.SALARY
                        else -> WorkerFilter.ALL
                    }
                )
            },
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        )
        if (data.workers.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(message = "No workers added yet", icon = Icons.Outlined.PersonOutline)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(data.workers, key = { it.worker.id }) { item ->
                    WorkerRow(
                        item = item,
                        onClick = { onEditWorker(item.worker.id) },
                        onHistory = { onTypeHistory(item.worker.id) }
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
    onHistory: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceCard)
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        WorkerAvatar(name = item.worker.name, size = 36.dp, fontSize = 11)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.worker.name,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.W500
            )
            val typeStr = if (item.worker.currentType == WorkerType.PIECE)
                "Piece • ${item.worker.phone}"
            else
                "Salary • ${Money.formatRupees(item.worker.dailyRate)}/day"
            Text(text = typeStr, color = TextSecondary, fontSize = 10.sp)
        }
        when (item.isPresentToday) {
            true -> StatusBadge("Present", BadgeType.SUCCESS)
            false -> StatusBadge("Absent", BadgeType.ERROR)
            null -> StatusBadge("—", BadgeType.INFO)
        }
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Color.Transparent)
                .clickable { onHistory() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.History, contentDescription = "History", tint = TextSecondary, modifier = Modifier.size(16.dp))
        }
    }
}
