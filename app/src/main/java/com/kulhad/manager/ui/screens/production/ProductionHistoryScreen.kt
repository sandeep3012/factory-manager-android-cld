package com.kulhad.manager.ui.screens.production

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
import com.kulhad.manager.data.util.DateUtils
import com.kulhad.manager.data.util.Money
import com.kulhad.manager.ui.charts.HorizontalBarChart
import com.kulhad.manager.ui.components.KpiStrip
import com.kulhad.manager.ui.components.KulhadTopBar
import com.kulhad.manager.ui.components.SectionHeader
import com.kulhad.manager.ui.components.WorkerAvatar
import com.kulhad.manager.ui.preview.UiDemoData
import com.kulhad.manager.ui.theme.BgDeep
import com.kulhad.manager.ui.theme.OverlayWhite07
import com.kulhad.manager.ui.theme.PurpleAccent
import com.kulhad.manager.ui.theme.Success
import com.kulhad.manager.ui.theme.SurfaceCard
import com.kulhad.manager.ui.theme.TextPrimary
import com.kulhad.manager.ui.theme.TextSecondary
import com.kulhad.manager.ui.theme.WarningAmber

@Composable
fun ProductionHistoryScreen(
    onBack: () -> Unit,
    viewModel: ProductionViewModel = hiltViewModel()
) {
    val month by viewModel.historyMonth.collectAsStateWithLifecycle()
    val entries by viewModel.historyEntries.collectAsStateWithLifecycle()

    val useDemo = UiDemoData.SHOW_DEMO && entries.isEmpty()

    val total     = if (useDemo) UiDemoData.productionTotal7d  else entries.sumOf { it.quantityProduced }
    val defective = if (useDemo) UiDemoData.productionDefective else entries.sumOf { it.defectiveQuantity }
    val quality   = if (useDemo) UiDemoData.productionQuality   else
        if (total == 0) 100 else ((total - defective) * 100) / total

    val topSizes  = if (useDemo) {
        UiDemoData.prodBySize.zip(UiDemoData.prodSizeLabels) { v, l -> "${l}ml" to v }.take(5)
    } else {
        entries
            .groupBy { it.productSize }
            .map { (size, list) -> "${size}ml" to list.sumOf { it.netQty }.toFloat() }
            .sortedByDescending { it.second }
            .take(5)
    }

    Column(modifier = Modifier.fillMaxSize().background(BgDeep)) {
        KulhadTopBar(
            title = "Production History",
            subtitle = DateUtils.formatMonth(month),
            onBack = onBack
        )
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // KPI strip
            item {
                KpiStrip(
                    items = listOf(
                        Triple((total - defective).toString(), "Total pieces", TextPrimary),
                        Triple(defective.toString(),           "Defective",    WarningAmber),
                        Triple("$quality%",                   "Quality",      Success)
                    )
                )
            }

            // Top sizes chart
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

            // Entries — flat rows with dividers
            item { SectionHeader(text = "Entries") }

            if (useDemo) {
                // Demo production entries
                val demoEntries = listOf(
                    Triple("Priya Devi",    "100ml · 10 May", "204 pcs • def 6"),
                    Triple("Ramesh Kumar",  "80ml · 10 May",  "182 pcs • def 4"),
                    Triple("Sunita Patel",  "60ml · 9 May",   "156 pcs • def 2"),
                    Triple("Mohan Kashyap","120ml · 9 May",  "130 pcs • def 8"),
                    Triple("Raj Verma",     "100ml · 8 May",  "118 pcs • def 3"),
                )
                items(demoEntries) { (name, meta, detail) ->
                    val isLast = name == demoEntries.last().first
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            WorkerAvatar(name = name, size = 30.dp, fontSize = 9)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(name, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.W500)
                                Text(meta, color = TextSecondary, fontSize = 10.sp)
                                Text(detail, color = TextSecondary, fontSize = 10.sp)
                            }
                            Text(
                                text = "₹2,448",
                                color = Success, fontSize = 12.sp, fontWeight = FontWeight.W600
                            )
                        }
                        if (!isLast) {
                            Box(Modifier.fillMaxWidth().height(0.5.dp).background(OverlayWhite07))
                        }
                    }
                }
            } else if (entries.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No production entries yet", color = TextSecondary, fontSize = 12.sp)
                    }
                }
            } else {
                items(entries, key = { it.id }) { e ->
                    val isLast = e == entries.last()
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            WorkerAvatar(name = e.workerName, size = 30.dp, fontSize = 9)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(e.workerName, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.W500)
                                Text(
                                    "${e.productSize}ml • ${DateUtils.formatDayShort(e.date)}",
                                    color = TextSecondary, fontSize = 10.sp
                                )
                                Text(
                                    "${e.quantityProduced} pcs • def ${e.defectiveQuantity}",
                                    color = TextSecondary, fontSize = 10.sp
                                )
                            }
                            Text(
                                text = Money.formatRupeesDouble(e.earnings),
                                color = Success, fontSize = 12.sp, fontWeight = FontWeight.W600
                            )
                        }
                        if (!isLast) {
                            Box(Modifier.fillMaxWidth().height(0.5.dp).background(OverlayWhite07))
                        }
                    }
                }
            }
        }
    }
}
