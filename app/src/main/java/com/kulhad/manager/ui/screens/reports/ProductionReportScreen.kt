package com.kulhad.manager.ui.screens.reports

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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.kulhad.manager.ui.charts.HorizontalBarChart
import com.kulhad.manager.ui.charts.SimpleBarChart
import com.kulhad.manager.ui.components.KpiStrip
import com.kulhad.manager.ui.components.KulhadTopBar
import com.kulhad.manager.ui.components.SectionHeader
import com.kulhad.manager.ui.preview.UiDemoData
import com.kulhad.manager.ui.theme.BgDeep
import com.kulhad.manager.ui.theme.ErrorRed
import com.kulhad.manager.ui.theme.OverlayWhite07
import com.kulhad.manager.ui.theme.PurpleAccent
import com.kulhad.manager.ui.theme.Success
import com.kulhad.manager.ui.theme.SurfaceCard
import com.kulhad.manager.ui.theme.TextPrimary
import com.kulhad.manager.ui.theme.TextSecondary
import com.kulhad.manager.ui.theme.WarningAmber

@Composable
fun ProductionReportScreen(
    onBack: () -> Unit,
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val month by viewModel.monthAnchor.collectAsStateWithLifecycle()
    val report by viewModel.production.collectAsStateWithLifecycle()

    LaunchedEffect(month) { viewModel.loadProduction() }

    val useDemo = UiDemoData.SHOW_DEMO && report == null

    val dispTotal   = if (useDemo) UiDemoData.productionTotal7d  else (report?.totalPieces ?: 0)
    val dispDefect  = if (useDemo) UiDemoData.productionDefective else (report?.defectivePieces ?: 0)
    val dispQuality = if (useDemo) "${"%.1f".format(UiDemoData.productionQuality.toDouble())}%" else "${"%.1f".format(report?.qualityPercent ?: 0.0)}%"

    val dispBySize  = if (useDemo) UiDemoData.prodBySize.zip(UiDemoData.prodSizeLabels) { v, l -> l to v }
                      else report?.bySize.orEmpty().map { "${it.sizeMl}ml" to it.qty.toFloat() }

    val dispWorkers = if (useDemo) UiDemoData.topWorkers.map { it.name to it.pieces }
                      else report?.byWorker.orEmpty().sortedByDescending { it.qty }.take(5).map { it.workerName to it.qty.toFloat() }

    val dispDaily   = report?.daily.orEmpty()

    Column(modifier = Modifier.fillMaxSize().background(BgDeep)) {
        KulhadTopBar(
            title = "Production Report",
            subtitle = DateUtils.formatMonth(month),
            onBack = onBack,
            actions = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Prev",
                    tint = TextPrimary,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { viewModel.prevMonth() }
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next",
                    tint = TextPrimary,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { viewModel.nextMonth() }
                )
            }
        )

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // KPI strip
            item {
                KpiStrip(
                    items = listOf(
                        Triple(dispTotal.toString(),  "Total pieces", Success),
                        Triple(dispDefect.toString(), "Defective",    ErrorRed),
                        Triple(dispQuality,           "Quality",      PurpleAccent)
                    )
                )
            }

            // Bar chart by size
            item { SectionHeader(text = "By product size") }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceCard)
                        .padding(12.dp)
                ) {
                    Text("PRODUCTION BY SIZE (ml)", color = TextSecondary, fontSize = 10.sp, letterSpacing = 0.5.sp)
                    androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
                    if (dispBySize.isEmpty()) {
                        Text("No production data this month", color = TextSecondary, fontSize = 13.sp)
                    } else {
                        SimpleBarChart(
                            values = dispBySize.map { it.second },
                            labels = dispBySize.map { it.first },
                            barColor = PurpleAccent,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Top workers horizontal bar chart
            item { SectionHeader(text = "Top workers") }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceCard)
                        .padding(12.dp)
                ) {
                    if (dispWorkers.isEmpty()) {
                        Text("No worker data this month", color = TextSecondary, fontSize = 13.sp)
                    } else {
                        HorizontalBarChart(items = dispWorkers, barColor = WarningAmber)
                    }
                }
            }

            // Daily output — flat rows with dividers
            item { SectionHeader(text = "Daily output") }

            if (useDemo) {
                // Demo daily entries placeholder
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(SurfaceCard)
                            .padding(horizontal = 12.dp)
                    ) {
                        val demoDays = listOf(
                            "10 May" to "550 pcs", "9 May" to "410 pcs",
                            "8 May" to "490 pcs", "7 May" to "380 pcs",
                            "6 May" to "260 pcs"
                        )
                        demoDays.forEachIndexed { idx, (day, qty) ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(day, color = TextSecondary, fontSize = 13.sp)
                                Text(qty, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.W500)
                            }
                            if (idx < demoDays.lastIndex) {
                                Box(Modifier.fillMaxWidth().height(0.5.dp).background(OverlayWhite07))
                            }
                        }
                    }
                }
            } else if (dispDaily.isEmpty()) {
                item {
                    Text(
                        text = "No entries recorded this month",
                        color = TextSecondary, fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            } else {
                items(dispDaily, key = { it.day }) { entry ->
                    val isLast = entry == dispDaily.last()
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = DateUtils.formatDayShort(entry.day),
                                color = TextSecondary, fontSize = 13.sp
                            )
                            Text(
                                text = "${entry.qty} pcs",
                                color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.W500
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
