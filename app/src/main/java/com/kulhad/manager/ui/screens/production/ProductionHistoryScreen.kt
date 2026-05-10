package com.kulhad.manager.ui.screens.production

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inventory
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
import com.kulhad.manager.data.util.DateUtils
import com.kulhad.manager.data.util.Money
import com.kulhad.manager.ui.charts.HorizontalBarChart
import com.kulhad.manager.ui.components.EmptyState
import com.kulhad.manager.ui.components.KulhadTopBar
import com.kulhad.manager.ui.components.SectionHeader
import com.kulhad.manager.ui.components.WorkerAvatar
import com.kulhad.manager.ui.theme.BgDeep
import com.kulhad.manager.ui.theme.PurpleAccent
import com.kulhad.manager.ui.theme.Success
import com.kulhad.manager.ui.theme.SurfaceCard
import com.kulhad.manager.ui.theme.TextPrimary
import com.kulhad.manager.ui.theme.TextSecondary

@Composable
fun ProductionHistoryScreen(
    onBack: () -> Unit,
    viewModel: ProductionViewModel = hiltViewModel()
) {
    val month by viewModel.historyMonth.collectAsStateWithLifecycle()
    val entries by viewModel.historyEntries.collectAsStateWithLifecycle()

    val total = entries.sumOf { it.quantityProduced }
    val defective = entries.sumOf { it.defectiveQuantity }
    val quality = if (total == 0) 100 else ((total - defective) * 100) / total

    val topSizes = entries
        .groupBy { it.productSize }
        .map { (size, list) -> "${size}ml" to list.sumOf { it.netQty }.toFloat() }
        .sortedByDescending { it.second }
        .take(5)

    Column(modifier = Modifier.fillMaxSize().background(BgDeep)) {
        KulhadTopBar(
            title = "Production History",
            subtitle = DateUtils.formatMonth(month),
            onBack = onBack
        )
        LazyColumn(
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    com.kulhad.manager.ui.components.StatCard(
                        value = (total - defective).toString(),
                        label = "Total pieces",
                        valueColor = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    com.kulhad.manager.ui.components.StatCard(
                        value = defective.toString(),
                        label = "Defective",
                        valueColor = androidx.compose.ui.graphics.Color(0xFFFBBF24),
                        modifier = Modifier.weight(1f)
                    )
                    com.kulhad.manager.ui.components.StatCard(
                        value = "$quality%",
                        label = "Quality",
                        valueColor = Success,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            item { SectionHeader(text = "Top sizes by volume") }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceCard)
                        .padding(12.dp)
                ) {
                    if (topSizes.isEmpty()) {
                        Text("No production this month", color = TextSecondary, fontSize = 11.sp)
                    } else {
                        HorizontalBarChart(items = topSizes, barColor = PurpleAccent)
                    }
                }
            }
            item { SectionHeader(text = "Entries") }
            if (entries.isEmpty()) {
                item { EmptyState(message = "No production entries yet", icon = Icons.Outlined.Inventory) }
            } else {
                items(entries, key = { it.id }) { e ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceCard)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        WorkerAvatar(name = e.workerName, size = 30.dp, fontSize = 9)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = e.workerName, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.W500)
                            Text(
                                text = "${e.productSize}ml • ${DateUtils.formatDayShort(e.date)}",
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                            Text(
                                text = "${e.quantityProduced} pcs • def ${e.defectiveQuantity}",
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                        Text(
                            text = Money.formatRupeesDouble(e.earnings),
                            color = Success,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.W600
                        )
                    }
                }
            }
        }
    }
}
