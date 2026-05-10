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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kulhad.manager.data.util.DateUtils
import com.kulhad.manager.data.util.Money
import com.kulhad.manager.ui.components.EmptyState
import com.kulhad.manager.ui.components.KulhadButton
import com.kulhad.manager.ui.components.KulhadTextField
import com.kulhad.manager.ui.components.KulhadTopBar
import com.kulhad.manager.ui.components.StatCard
import com.kulhad.manager.ui.theme.BgDeep
import com.kulhad.manager.ui.theme.ErrorRed
import com.kulhad.manager.ui.theme.SurfaceCard
import com.kulhad.manager.ui.theme.TextPrimary
import com.kulhad.manager.ui.theme.TextSecondary

@Composable
fun AdvanceEntryScreen(
    initialWorkerId: Long?,
    onBack: () -> Unit,
    viewModel: WorkerViewModel = hiltViewModel()
) {
    val workers by viewModel.activeWorkers.collectAsStateWithLifecycle()
    var selectedId by remember { mutableStateOf(initialWorkerId) }
    var amount by remember { mutableStateOf("") }
    var remark by remember { mutableStateOf("") }
    val date = DateUtils.todayStart()

    val effectiveId = selectedId ?: workers.firstOrNull()?.id
    val advances by (effectiveId?.let { viewModel.observeAdvances(it) }
        ?: kotlinx.coroutines.flow.flowOf(emptyList())).collectAsStateWithLifecycle(emptyList())
    val monthTotal by (effectiveId?.let { viewModel.observeAdvanceTotalThisMonth(it) }
        ?: kotlinx.coroutines.flow.flowOf(0)).collectAsStateWithLifecycle(0)
    val workerName = workers.firstOrNull { it.id == effectiveId }?.name ?: "Worker"

    Column(modifier = Modifier.fillMaxSize().background(BgDeep)) {
        KulhadTopBar(title = "Advance Entry", onBack = onBack)
        LazyColumn(
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text("WORKER", color = TextSecondary, fontSize = 9.sp, letterSpacing = 0.5.sp)
            }
            items(workers, key = { it.id }) { w ->
                val sel = w.id == effectiveId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (sel) Color(0xFF1E3A5F) else SurfaceCard)
                        .clickable { selectedId = w.id }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = w.name,
                        color = if (sel) TextPrimary else TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = if (sel) FontWeight.W600 else FontWeight.W500,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = if (w.currentType.name == "PIECE") "Piece" else "Salary",
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }
            }
            item {
                KulhadTextField(
                    label = "Amount (₹)",
                    value = amount,
                    onValueChange = { amount = it.filter { ch -> ch.isDigit() } },
                    keyboardType = KeyboardType.Number
                )
            }
            item {
                KulhadTextField(
                    label = "Remark",
                    value = remark,
                    onValueChange = { remark = it }
                )
            }
            item {
                Text(
                    text = "Date: ${DateUtils.formatDay(date)}",
                    color = TextSecondary,
                    fontSize = 10.sp
                )
            }
            item {
                StatCard(
                    value = Money.formatRupees(monthTotal),
                    label = "Advances this month",
                    valueColor = ErrorRed,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                KulhadButton(
                    text = "Save Advance",
                    onClick = {
                        val amt = amount.toIntOrNull() ?: return@KulhadButton
                        if (amt <= 0 || effectiveId == null) return@KulhadButton
                        viewModel.saveAdvance(effectiveId, amt, date, remark) {
                            amount = ""
                            remark = ""
                        }
                    }
                )
            }
            item {
                Text(
                    text = "ADVANCE HISTORY • $workerName",
                    color = TextSecondary,
                    fontSize = 9.sp,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            if (advances.isEmpty()) {
                item {
                    EmptyState(message = "No advances yet", icon = Icons.Outlined.Savings)
                }
            } else {
                items(advances, key = { it.id }) { a ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceCard)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = Money.formatRupees(a.amount), color = ErrorRed,
                                fontSize = 13.sp, fontWeight = FontWeight.W500)
                            if (a.remark.isNotBlank()) {
                                Text(text = a.remark, color = TextSecondary, fontSize = 10.sp)
                            }
                        }
                        Text(
                            text = DateUtils.formatDayShort(a.date),
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}
