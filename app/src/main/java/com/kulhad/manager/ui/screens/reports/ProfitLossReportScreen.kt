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
import com.kulhad.manager.data.util.Money
import com.kulhad.manager.ui.charts.DonutChart
import com.kulhad.manager.ui.charts.SimpleLineChart
import com.kulhad.manager.ui.components.KulhadTopBar
import com.kulhad.manager.ui.components.SectionHeader
import com.kulhad.manager.ui.theme.BgDeep
import com.kulhad.manager.ui.theme.ErrorRed
import com.kulhad.manager.ui.theme.Success
import com.kulhad.manager.ui.theme.SurfaceCard
import com.kulhad.manager.ui.theme.TextPrimary
import com.kulhad.manager.ui.theme.TextSecondary
import com.kulhad.manager.ui.theme.WarningAmber

@Composable
fun ProfitLossReportScreen(
    onBack: () -> Unit,
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val month by viewModel.monthAnchor.collectAsStateWithLifecycle()
    val report by viewModel.profitLoss.collectAsStateWithLifecycle()

    LaunchedEffect(month) { viewModel.loadProfitLoss() }

    Column(modifier = Modifier.fillMaxSize().background(BgDeep)) {
        KulhadTopBar(
            title = "Profit & Loss",
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
            // Hero card: net profit + donut
            item {
                val net = report?.netProfit ?: 0
                val totalSales = report?.totalSales ?: 0
                val totalCosts = report?.totalExpenses ?: 0
                val isProfit = net >= 0
                val netColor = if (isProfit) Success else ErrorRed
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceCard)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = if (isProfit) "Net Profit" else "Net Loss",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = Money.formatRupees(net),
                            color = netColor,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold
                        )
                        val pct = report?.percentChange ?: 0.0
                        val arrow = if (pct >= 0) "▲" else "▼"
                        val pctColor = if (pct >= 0) Success else ErrorRed
                        if (report != null) {
                            Text(
                                text = "$arrow ${"%.1f".format(kotlin.math.abs(pct))}% vs last month",
                                color = pctColor,
                                fontSize = 10.sp
                            )
                        }
                    }
                    DonutChart(
                        primaryValue = totalSales.toFloat().coerceAtLeast(0f),
                        secondaryValue = totalCosts.toFloat().coerceAtLeast(0f),
                        primaryColor = Success,
                        secondaryColor = ErrorRed,
                        size = 72.dp,
                        strokeWidth = 10.dp
                    )
                }
            }

            // 4-month trend line chart
            item { SectionHeader(text = "Monthly trend") }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceCard)
                        .padding(12.dp)
                ) {
                    val trend = report?.monthlyTrend.orEmpty()
                    if (trend.isEmpty()) {
                        Text("No trend data yet", color = TextSecondary, fontSize = 11.sp)
                    } else {
                        val values = trend.map { (_, profit) -> profit.toFloat() }
                        val labels = trend.map { (label, _) -> label }
                        SimpleLineChart(
                            values = values,
                            lineColor = Success,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            labels.forEach { lbl ->
                                Text(text = lbl, color = TextSecondary, fontSize = 9.sp)
                            }
                        }
                    }
                }
            }

            // Breakdown list
            item { SectionHeader(text = "Breakdown") }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceCard)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BreakdownRow(
                        label = "Total sales",
                        value = Money.formatRupees(report?.totalSales ?: 0),
                        valueColor = Success
                    )
                    BreakdownRow(
                        label = "Labor cost",
                        value = "−${Money.formatRupees(report?.laborCost ?: 0)}",
                        valueColor = ErrorRed
                    )
                    report?.expenseByType.orEmpty().forEach { (typeName, amt) ->
                        BreakdownRow(
                            label = typeName,
                            value = "−${Money.formatRupees(amt)}",
                            valueColor = WarningAmber
                        )
                    }
                    // Divider
                    androidx.compose.foundation.layout.Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    )
                    val net = report?.netProfit ?: 0
                    val netColor = if (net >= 0) Success else ErrorRed
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (net >= 0) "Net profit" else "Net loss",
                            color = netColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = Money.formatRupees(net),
                            color = netColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BreakdownRow(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = TextSecondary, fontSize = 12.sp)
        Text(text = value, color = valueColor, fontSize = 12.sp, fontWeight = FontWeight.W500)
    }
}
