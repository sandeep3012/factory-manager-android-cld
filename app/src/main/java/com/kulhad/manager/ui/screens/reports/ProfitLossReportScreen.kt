package com.kulhad.manager.ui.screens.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kulhad.manager.data.util.DateUtils
import com.kulhad.manager.data.util.Money
import com.kulhad.manager.domain.model.MonthSummary
import com.kulhad.manager.ui.charts.DonutChart
import com.kulhad.manager.ui.charts.SimpleBarChart
import com.kulhad.manager.ui.charts.SimpleLineChart
import com.kulhad.manager.ui.components.KulhadTopBar
import com.kulhad.manager.ui.components.ReportRow
import com.kulhad.manager.ui.components.SectionHeader
import com.kulhad.manager.ui.theme.BgDeep
import com.kulhad.manager.ui.theme.ErrorRed
import com.kulhad.manager.ui.theme.InfoBlue
import com.kulhad.manager.ui.theme.OverlayWhite07
import com.kulhad.manager.ui.theme.OverlayWhite15
import com.kulhad.manager.ui.theme.Success
import com.kulhad.manager.ui.theme.SurfaceCard
import com.kulhad.manager.ui.theme.TextPrimary
import com.kulhad.manager.ui.theme.TextSecondary
import com.kulhad.manager.ui.theme.TextTertiary
import com.kulhad.manager.ui.theme.WarningAmber

// ─────────────────────────────────────────────────────────────────────────────
// Public entry point
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ProfitLossReportScreen(
    onBack: () -> Unit,
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val month  by viewModel.monthAnchor.collectAsStateWithLifecycle()
    val report by viewModel.profitLoss.collectAsStateWithLifecycle()

    LaunchedEffect(month) { viewModel.loadProfitLoss() }

    // ── Resolved values (default to 0 while loading) ──────────────────────
    val revenue     = report?.totalSales?.toLong()     ?: 0L
    val labourCost  = report?.laborCost?.toLong()      ?: 0L
    val expenses    = report?.totalExpenses?.toLong()  ?: 0L
    val grossProfit = report?.grossProfit?.toLong()    ?: 0L
    val net         = report?.netProfit?.toLong()      ?: 0L
    val pctChange   = report?.percentChange            ?: 0.0
    val expenseByType = report?.expenseByType.orEmpty()
    val trendFull   = report?.trendFull.orEmpty()
    val prevRevenue = report?.previousRevenue?.toLong()   ?: 0L
    val prevLabour  = report?.previousLaborCost?.toLong() ?: 0L
    val prevExpenses = report?.previousExpenses?.toLong() ?: 0L
    val prevProfit  = report?.previousProfit?.toLong()   ?: 0L

    val isProfit = net >= 0
    val netColor = if (isProfit) Success else ErrorRed
    val profitPct: Double? =
        if (revenue > 0L) (net.toDouble() / revenue.toDouble()) * 100.0 else null

    Column(modifier = Modifier.fillMaxSize().background(BgDeep)) {
        KulhadTopBar(
            title    = "Profit & Loss",
            subtitle = DateUtils.formatMonth(month),
            onBack   = onBack,
            actions  = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Previous month",
                    tint = TextPrimary,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { viewModel.prevMonth() }
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next month",
                    tint = TextPrimary,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { viewModel.nextMonth() }
                )
            }
        )

        LazyColumn(
            contentPadding      = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            // ── 1. Hero card ──────────────────────────────────────────────
            item {
                HeroCard(
                    net         = net,
                    revenue     = revenue,
                    labourCost  = labourCost,
                    expenses    = expenses,
                    profitPct   = profitPct,
                    pctChange   = pctChange,
                    isProfit    = isProfit,
                    netColor    = netColor
                )
            }

            // ── 2. Monthly trend ─────────────────────────────────────────
            item { SectionHeader(text = "Monthly Trend") }
            item {
                TrendCard(trend = trendFull)
            }

            // ── 3. Breakdown ─────────────────────────────────────────────
            item { SectionHeader(text = "Breakdown") }
            item {
                BreakdownCard(
                    revenue     = revenue,
                    labourCost  = labourCost,
                    expenses    = expenses,
                    grossProfit = grossProfit,
                    net         = net,
                    isProfit    = isProfit,
                    netColor    = netColor
                )
            }

            // ── 4. Expense Category Breakdown ────────────────────────────
            val activeExpenses = expenseByType.filter { it.second > 0 }
            if (activeExpenses.isNotEmpty()) {
                item { SectionHeader(text = "Expense Breakdown") }
                item {
                    ExpenseCategoryCard(items = activeExpenses)
                }
            }

            // ── 5. Month Comparison ──────────────────────────────────────
            if (report != null) {
                item { SectionHeader(text = "This Month vs Last Month") }
                item {
                    MonthComparisonCard(
                        currRevenue  = revenue,
                        prevRevenue  = prevRevenue,
                        currLabour   = labourCost,
                        prevLabour   = prevLabour,
                        currExpenses = expenses,
                        prevExpenses = prevExpenses,
                        currProfit   = net,
                        prevProfit   = prevProfit
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Hero card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HeroCard(
    net: Long,
    revenue: Long,
    labourCost: Long,
    expenses: Long,
    profitPct: Double?,
    pctChange: Double,
    isProfit: Boolean,
    netColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(17.dp))
            .background(SurfaceCard)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text          = "NET ${if (isProfit) "PROFIT" else "LOSS"}",
                color         = TextSecondary,
                fontSize      = 11.sp,
                letterSpacing = 0.8.sp
            )
            Text(
                text       = Money.formatRupees(net),
                color      = netColor,
                fontSize   = 30.sp,
                fontWeight = FontWeight.Bold
            )

            // Profit % badge
            if (profitPct != null) {
                val pctFormatted = "${"%.1f".format(profitPct)}% margin"
                Text(
                    text       = pctFormatted,
                    color      = netColor,
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.W500
                )
            }

            // % change vs last month
            val arrow    = if (pctChange >= 0) "▲" else "▼"
            val pctColor = if (pctChange >= 0) Success else ErrorRed
            Text(
                text     = "$arrow ${"%.1f".format(kotlin.math.abs(pctChange))}% vs last month",
                color    = pctColor,
                fontSize = 12.sp
            )

            Spacer(Modifier.height(6.dp))

            // Three-column mini stat row
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MiniStat(label = "Revenue", amount = revenue, color = Success)
                MiniStat(label = "Labour",  amount = labourCost, color = WarningAmber)
                MiniStat(label = "Expenses", amount = expenses,  color = ErrorRed)
            }
        }

        Spacer(Modifier.width(10.dp))

        DonutChart(
            primaryValue   = revenue.toFloat().coerceAtLeast(0.1f),
            secondaryValue = (labourCost + expenses).toFloat().coerceAtLeast(0.1f),
            primaryColor   = Success,
            secondaryColor = ErrorRed,
            size           = 82.dp,
            strokeWidth    = 11.dp
        )
    }
}

@Composable
private fun MiniStat(label: String, amount: Long, color: Color) {
    Column {
        Text(text = label, color = TextSecondary, fontSize = 10.sp)
        Text(
            text       = Money.formatRupees(amount),
            color      = color,
            fontSize   = 11.sp,
            fontWeight = FontWeight.W600
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 6-month trend card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TrendCard(trend: List<MonthSummary>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceCard)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Legend
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            LegendDot(color = Success,      label = "Revenue")
            LegendDot(color = InfoBlue,     label = "Net Profit")
            LegendDot(color = WarningAmber, label = "Labour")
            LegendDot(color = ErrorRed,     label = "Expenses")
        }

        if (trend.isEmpty()) {
            Text("No trend data yet", color = TextSecondary, fontSize = 13.sp)
        } else {
            val revVals  = trend.map { it.revenue.toFloat() }
            val netVals  = trend.map { it.netProfit.toFloat() }
            val labVals  = trend.map { it.labourCost.toFloat() }
            val expVals  = trend.map { it.expenses.toFloat() }
            val labels   = trend.map {
                // Shorten "Jun 2025" → "Jun"
                it.label.split(" ").firstOrNull() ?: it.label
            }

            // Revenue — bar chart
            SimpleBarChart(
                values     = revVals,
                labels     = emptyList(),
                barColor   = Success,
                chartHeight = 64.dp,
                showLabels = false,
                modifier   = Modifier.fillMaxWidth()
            )

            // Net Profit — line chart
            SimpleLineChart(
                values      = netVals,
                lineColor   = InfoBlue,
                chartHeight = 48.dp,
                showGrid    = false,
                modifier    = Modifier.fillMaxWidth()
            )

            // Labour — line chart
            SimpleLineChart(
                values      = labVals,
                lineColor   = WarningAmber,
                chartHeight = 40.dp,
                showGrid    = false,
                modifier    = Modifier.fillMaxWidth()
            )

            // Expenses — line chart
            SimpleLineChart(
                values      = expVals,
                lineColor   = ErrorRed,
                chartHeight = 40.dp,
                showGrid    = false,
                modifier    = Modifier.fillMaxWidth()
            )

            // Month labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                labels.forEach { lbl ->
                    Text(
                        text      = lbl,
                        color     = TextTertiary,
                        fontSize  = 10.sp,
                        textAlign = TextAlign.Center,
                        modifier  = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(
        verticalAlignment    = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color)
        )
        Text(text = label, color = TextSecondary, fontSize = 10.sp)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Breakdown card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BreakdownCard(
    revenue: Long,
    labourCost: Long,
    expenses: Long,
    grossProfit: Long,
    net: Long,
    isProfit: Boolean,
    netColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceCard)
            .padding(horizontal = 12.dp)
    ) {
        ReportRow(
            label       = "Revenue",
            value       = Money.formatRupees(revenue),
            valueColor  = Success,
            showDivider = true
        )
        ReportRow(
            label       = "Labour Cost",
            value       = "−${Money.formatRupees(labourCost)}",
            valueColor  = WarningAmber,
            showDivider = true
        )
        ReportRow(
            label       = "Expenses",
            value       = "−${Money.formatRupees(expenses)}",
            valueColor  = ErrorRed,
            showDivider = true
        )
        // Gross profit divider accent
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(OverlayWhite15)
        )
        ReportRow(
            label       = "Gross Profit",
            value       = Money.formatRupees(grossProfit),
            valueColor  = if (grossProfit >= 0) Success else ErrorRed,
            showDivider = true
        )
        ReportRow(
            label       = if (isProfit) "Net Profit" else "Net Loss",
            value       = Money.formatRupees(net),
            valueColor  = netColor,
            bold        = true,
            showDivider = false
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Expense Category Breakdown card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ExpenseCategoryCard(items: List<Pair<String, Int>>) {
    // items already sorted desc and filtered by caller; sort again to be safe
    val sorted = items.sortedByDescending { it.second }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceCard)
            .padding(horizontal = 12.dp)
    ) {
        sorted.forEachIndexed { idx, (name, amount) ->
            ReportRow(
                label       = name,
                value       = "−${Money.formatRupees(amount.toLong())}",
                valueColor  = WarningAmber,
                showDivider = idx < sorted.lastIndex
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Month Comparison card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MonthComparisonCard(
    currRevenue:  Long,
    prevRevenue:  Long,
    currLabour:   Long,
    prevLabour:   Long,
    currExpenses: Long,
    prevExpenses: Long,
    currProfit:   Long,
    prevProfit:   Long
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceCard)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        ComparisonRow(
            label           = "Revenue",
            prev            = prevRevenue,
            curr            = currRevenue,
            positiveIsGood  = true,
            showDivider     = true
        )
        ComparisonRow(
            label           = "Labour Cost",
            prev            = prevLabour,
            curr            = currLabour,
            positiveIsGood  = false,   // cost increase = bad
            showDivider     = true
        )
        ComparisonRow(
            label           = "Expenses",
            prev            = prevExpenses,
            curr            = currExpenses,
            positiveIsGood  = false,
            showDivider     = true
        )
        ComparisonRow(
            label           = "Net Profit",
            prev            = prevProfit,
            curr            = currProfit,
            positiveIsGood  = true,
            showDivider     = false
        )
    }
}

/**
 * One row in the comparison card.
 *
 * Shows: Label / prev → curr / ±X%
 *
 * [positiveIsGood] controls the arrow colour: for Revenue and Net Profit an
 * increase is favourable (green); for Labour and Expenses an increase is
 * unfavourable (red).
 */
@Composable
private fun ComparisonRow(
    label: String,
    prev: Long,
    curr: Long,
    positiveIsGood: Boolean,
    showDivider: Boolean
) {
    val diff = curr - prev
    val pct  = if (prev == 0L) 0.0
               else (diff.toDouble() / kotlin.math.abs(prev.toDouble())) * 100.0
    val isUp = diff >= 0
    // "isPositive" = is this change a good thing?
    val isPositive = if (positiveIsGood) isUp else !isUp
    val arrowColor = if (isPositive) Success else ErrorRed
    val arrow      = if (isUp) "▲" else "▼"
    val pctText    = "$arrow ${"%.1f".format(kotlin.math.abs(pct))}%"

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier  = Modifier
                .fillMaxWidth()
                .padding(vertical = 9.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = label, color = TextSecondary, fontSize = 11.sp)
                Spacer(Modifier.height(2.dp))
                Text(
                    text       = "${Money.formatRupees(prev)} → ${Money.formatRupees(curr)}",
                    color      = TextPrimary,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.W500
                )
            }
            Text(
                text       = pctText,
                color      = arrowColor,
                fontSize   = 13.sp,
                fontWeight = FontWeight.W600
            )
        }
        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(OverlayWhite07)
            )
        }
    }
}
