package com.kulhad.manager.ui.screens.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.kulhad.manager.ui.components.KulhadTopBar
import com.kulhad.manager.ui.components.SectionHeader
import com.kulhad.manager.ui.components.StatCard
import com.kulhad.manager.ui.theme.BgDeep
import com.kulhad.manager.ui.theme.ErrorRed
import com.kulhad.manager.ui.theme.PurpleAccent
import com.kulhad.manager.ui.theme.Success
import com.kulhad.manager.ui.theme.SurfaceCard
import com.kulhad.manager.ui.theme.TextPrimary
import com.kulhad.manager.ui.theme.TextSecondary

@Composable
fun ProductionReportScreen(
    onBack: () -> Unit,
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val month by viewModel.monthAnchor.collectAsStateWithLifecycle()
    val report by viewModel.production.collectAsStateWithLifecycle()

    LaunchedEffect(month) { viewModel.loadProduction() }

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
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // KPI strip
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard(
                        value = "${report?.totalPieces ?: 0}",
                        label = "Total pieces",
                        valueColor = Success,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        value = "${report?.defectivePieces ?: 0}",
                        label = "Defective",
                        valueColor = ErrorRed,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        value = "${"%.1f".format(report?.qualityPercent ?: 0.0)}%",
                        label = "Quality",
                        valueColor = PurpleAccent,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Bar chart by size
            item { SectionHeader(text = "By product size") }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceCard)
                        .padding(12.dp)
                ) {
                    val bySize = report?.bySize.orEmpty()
                    if (bySize.isEmpty()) {
                        Text("No production data this month", color = TextSecondary, fontSize = 11.sp)
                    } else {
                        val values = bySize.map { it.qty.toFloat() }
                        val labels = bySize.map { "${it.sizeMl}ml" }
                        SimpleBarChart(
                            values = values,
                            labels = labels,
                            barColor = PurpleAccent,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Horizontal bar chart by worker
            item { SectionHeader(text = "Top workers") }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceCard)
                        .padding(12.dp)
                ) {
                    val workers = report?.byWorker.orEmpty()
                        .sortedByDescending { it.qty }
                        .take(5)
                        .map { it.workerName to it.qty.toFloat() }
                    if (workers.isEmpty()) {
                        Text("No worker data this month", color = TextSecondary, fontSize = 11.sp)
                    } else {
                        HorizontalBarChart(items = workers, barColor = PurpleAccent)
                    }
                }
            }

            // Daily entries list
            item { SectionHeader(text = "Daily output") }
            val daily = report?.daily.orEmpty()
            if (daily.isEmpty()) {
                item {
                    Text(
                        text = "No entries recorded this month",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            } else {
                items(daily, key = { it.day }) { entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceCard)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = DateUtils.formatDayShort(entry.day),
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "${entry.qty} pcs",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.W500
                        )
                    }
                }
            }
        }
    }
}
