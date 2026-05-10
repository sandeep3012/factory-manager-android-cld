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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kulhad.manager.data.local.entity.WorkerType
import com.kulhad.manager.data.util.DateUtils
import com.kulhad.manager.data.util.Money
import com.kulhad.manager.domain.model.WorkerTypeChange
import com.kulhad.manager.ui.components.BadgeType
import com.kulhad.manager.ui.components.KulhadButton
import com.kulhad.manager.ui.components.KulhadTextField
import com.kulhad.manager.ui.components.KulhadTopBar
import com.kulhad.manager.ui.components.SegmentedControl
import com.kulhad.manager.ui.components.StatusBadge
import com.kulhad.manager.ui.theme.BgDeep
import com.kulhad.manager.ui.theme.SurfaceCard
import com.kulhad.manager.ui.theme.TextPrimary
import com.kulhad.manager.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerTypeHistoryScreen(
    workerId: Long,
    onBack: () -> Unit,
    viewModel: WorkerViewModel = hiltViewModel()
) {
    val history by viewModel.observeTypeHistory(workerId)
        .collectAsStateWithLifecycle(emptyList())
    val workerName by viewModel.observeWorkerName(workerId)
        .collectAsStateWithLifecycle("Worker")
    var sheetOpen by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    Column(modifier = Modifier.fillMaxSize().background(BgDeep)) {
        KulhadTopBar(title = "Type History", subtitle = workerName, onBack = onBack)
        LazyColumn(
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val current = history.firstOrNull()
            if (current != null) {
                item { Text("CURRENT", color = TextSecondary, fontSize = 9.sp) }
                item { TypeHistoryRow(current, isCurrent = true) }
            }
            if (history.size > 1) {
                item {
                    Text("PREVIOUS", color = TextSecondary, fontSize = 9.sp,
                        modifier = Modifier.padding(top = 6.dp))
                }
                items(history.drop(1), key = { it.id }) { row -> TypeHistoryRow(row, isCurrent = false) }
            }
            item {
                KulhadButton(
                    text = "Change Type / Rate",
                    onClick = { sheetOpen = true }
                )
            }
        }
    }

    if (sheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { sheetOpen = false },
            sheetState = sheetState,
            containerColor = SurfaceCard
        ) {
            ChangeTypeSheet(
                onSave = { type, rate, effectiveFrom ->
                    viewModel.changeType(workerId, type, rate, effectiveFrom) {
                        sheetOpen = false
                    }
                }
            )
        }
    }
}

@Composable
private fun TypeHistoryRow(row: WorkerTypeChange, isCurrent: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceCard)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            val typeLabel = if (row.workerType == WorkerType.PIECE) "Piece" else "Salary"
            Text(text = typeLabel, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.W500)
            val rateLine = if (row.workerType == WorkerType.SALARY)
                "${Money.formatRupees(row.dailyRate)}/day"
            else
                "Per piece (see piece rates)"
            Text(text = rateLine, color = TextSecondary, fontSize = 10.sp)
            Text(
                text = "From ${DateUtils.formatDay(row.effectiveFrom)}",
                color = TextSecondary,
                fontSize = 9.sp
            )
        }
        if (isCurrent) StatusBadge("Current", BadgeType.INFO)
    }
}

@Composable
private fun ChangeTypeSheet(
    onSave: (WorkerType, Int, Long) -> Unit
) {
    var typeStr by remember { mutableStateOf("Piece worker") }
    var rate by remember { mutableStateOf("") }
    val effectiveFrom = DateUtils.todayStart()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(text = "Change worker type", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.W500)
        SegmentedControl(
            options = listOf("Piece worker", "Salary worker"),
            selected = typeStr,
            onSelect = { typeStr = it }
        )
        if (typeStr == "Salary worker") {
            KulhadTextField(
                label = "Daily rate",
                value = rate,
                onValueChange = { rate = it.filter { ch -> ch.isDigit() } },
                keyboardType = KeyboardType.Number
            )
        }
        Text(
            text = "Effective from ${DateUtils.formatDay(effectiveFrom)}",
            color = TextSecondary, fontSize = 10.sp
        )
        KulhadButton(
            text = "Save change",
            onClick = {
                val type = if (typeStr == "Piece worker") WorkerType.PIECE else WorkerType.SALARY
                onSave(type, rate.toIntOrNull() ?: 0, effectiveFrom)
            }
        )
    }
}
