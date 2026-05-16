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
import com.kulhad.manager.ui.components.ReportRow
import com.kulhad.manager.ui.components.SectionHeader
import com.kulhad.manager.ui.preview.UiDemoData
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

    val useDemo = UiDemoData.SHOW_DEMO && report == null

    val net       = if (useDemo) UiDemoData.plNetProfit          else (report?.netProfit?.toLong() ?: 0L)
    val revenue   = if (useDemo) UiDemoData.plRevenue            else (report?.totalSales?.toLong() ?: 0L)
    val costs     = if (useDemo) UiDemoData.plLaborCost + UiDemoData.plOtherCosts else (report?.totalExpenses?.toLong() ?: 0L)
    val labor     = if (useDemo) UiDemoData.plLaborCost          else (report?.laborCost?.toLong() ?: 0L)
    val pctChange = if (useDemo) UiDemoData.plPercentChange      else (report?.percentChange ?: 0.0)
    val trend     = if (useDemo) UiDemoData.plMonthlyTrend       else report?.monthlyTrend?.map { it.second.toFloat() }.orEmpty()
    val trendLbls = if (useDemo) UiDemoData.plTrendLabels        else report?.monthlyTrend?.map { it.first }.orEmpty()
    val expenseByType: List<Pair<String, Long>> =
        if (useDemo) listOf("Soil" to 18_000L, "Transport" to 7_000L)
        else report?.expenseByType.orEmpty().map { (k, v) -> k to v.toLong() }

    val isProfit  = net >= 0
    val netColor  = if (isProfit) Success else ErrorRed

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
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Hero card: net profit + donut (HTML screen 8 style)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(17.dp))
                        .background(SurfaceCard)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "NET ${if (isProfit) "PROFIT" else "LOSS"} — THIS MONTH",
                            color = TextSecondary, fontSize = 14.sp, letterSpacing = 0.6.sp
                        )
                        Text(
                            text = Money.formatRupees(net),
                            color = netColor, fontSize = 31.sp, fontWeight = FontWeight.Bold
                        )
                        val arrow = if (pctChange >= 0) "▲" else "▼"
                        val pctColor = if (pctChange >= 0) Success else ErrorRed
                        Text(
                            text = "$arrow ${"%.1f".format(kotlin.math.abs(pctChange))}% vs last month",
                            color = pctColor, fontSize = 14.sp
                        )
                        androidx.compose.foundation.layout.Spacer(Modifier.size(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Column {
                                Text("Revenue", color = TextSecondary, fontSize = 14.sp)
                                Text(Money.formatRupees(revenue), color = Success, fontSize = 13.sp, fontWeight = FontWeight.W600)
                            }
                            Column {
                                Text("Costs", color = TextSecondary, fontSize = 14.sp)
                                Text(Money.formatRupees(costs), color = ErrorRed, fontSize = 13.sp, fontWeight = FontWeight.W600)
                            }
                        }
                    }
                    DonutChart(
                        primaryValue = revenue.toFloat().coerceAtLeast(0.1f),
                        secondaryValue = costs.toFloat().coerceAtLeast(0.1f),
                        primaryColor = Success,
                        secondaryColor = ErrorRed,
                        size = 86.dp,
                        strokeWidth = 12.dp
                    )
                }
            }

            // 4-month trend line chart
            item { SectionHeader(text = "Monthly trend") }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceCard)
                        .padding(12.dp)
                ) {
                    Text("NET PROFIT — LAST 4 MONTHS", color = TextSecondary, fontSize = 14.sp, letterSpacing = 0.5.sp)
                    androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
                    if (trend.isEmpty()) {
                        Text("No trend data yet", color = TextSecondary, fontSize = 13.sp)
                    } else {
                        SimpleLineChart(
                            values = trend,
                            lineColor = Success,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            trendLbls.forEach { lbl ->
                                Text(text = lbl, color = TextSecondary, fontSize = 9.sp)
                            }
                        }
                    }
                }
            }

            // Breakdown list — finance rows (HTML screen 8)
            item { SectionHeader(text = "Breakdown") }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceCard)
                        .padding(horizontal = 12.dp)
                ) {
                    ReportRow(
                        label = "Total sales",
                        value = Money.formatRupees(revenue),
                        valueColor = Success,
                        showDivider = true
                    )
                    ReportRow(
                        label = "Labor cost",
                        value = "−${Money.formatRupees(labor)}",
                        valueColor = ErrorRed,
                        showDivider = expenseByType.isNotEmpty()
                    )
                    expenseByType.forEachIndexed { idx, (name, amt) ->
                        ReportRow(
                            label = name,
                            value = "−${Money.formatRupees(amt)}",
                            valueColor = WarningAmber,
                            showDivider = idx < expenseByType.lastIndex
                        )
                    }
                    // Bold net row
                    ReportRow(
                        label = if (isProfit) "Net profit" else "Net loss",
                        value = Money.formatRupees(net),
                        valueColor = netColor,
                        bold = true,
                        showDivider = false
                    )
                }
            }
        }
    }
}
