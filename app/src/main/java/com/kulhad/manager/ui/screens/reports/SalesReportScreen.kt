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
import com.kulhad.manager.data.util.Money
import com.kulhad.manager.ui.charts.MultiSegmentDonut
import com.kulhad.manager.ui.charts.SimpleLineChart
import com.kulhad.manager.ui.components.KpiStrip
import com.kulhad.manager.ui.components.KulhadTopBar
import com.kulhad.manager.ui.components.SectionHeader
import com.kulhad.manager.ui.components.WorkerAvatar
import com.kulhad.manager.ui.preview.UiDemoData
import com.kulhad.manager.ui.theme.BgDeep
import com.kulhad.manager.ui.theme.ErrorRed
import com.kulhad.manager.ui.theme.OverlayWhite07
import com.kulhad.manager.ui.theme.PrimaryBlue
import com.kulhad.manager.ui.theme.Success
import com.kulhad.manager.ui.theme.SurfaceCard
import com.kulhad.manager.ui.theme.TextPrimary
import com.kulhad.manager.ui.theme.TextSecondary
import com.kulhad.manager.ui.theme.WarningAmber

@Composable
fun SalesReportScreen(
    onBack: () -> Unit,
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val month by viewModel.monthAnchor.collectAsStateWithLifecycle()
    val report by viewModel.sales.collectAsStateWithLifecycle()

    LaunchedEffect(month) { viewModel.loadSales() }

    val useDemo = UiDemoData.SHOW_DEMO && report == null

    val dispRevenue   = if (useDemo) UiDemoData.salesWeekTotal         else (report?.totalSales?.toLong() ?: 0L)
    val dispCollected = if (useDemo) UiDemoData.salesCollected         else (report?.collected?.toLong() ?: 0L)
    val dispPending   = if (useDemo) UiDemoData.salesPending           else (report?.pending?.toLong() ?: 0L)
    val dispOrders    = if (useDemo) UiDemoData.salesOrderCount        else (report?.orderCount ?: 0)
    val dispAvg       = if (useDemo) 6_316L                            else (report?.avgOrderValue?.toLong() ?: 0L)
    val dispDaily     = if (useDemo) UiDemoData.salesReportDaily else report?.daily.orEmpty().map { (_, v) -> v.toFloat() }
    val dispCustomers = if (useDemo) UiDemoData.topCustomers     else null

    Column(modifier = Modifier.fillMaxSize().background(BgDeep)) {
        KulhadTopBar(
            title = "Sales Report",
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
            // KPI strip — 3-col primary metrics
            item {
                KpiStrip(
                    items = listOf(
                        Triple(Money.formatRupees(dispRevenue),   "Revenue",   Success),
                        Triple(Money.formatRupees(dispCollected), "Collected", PrimaryBlue),
                        Triple(Money.formatRupees(dispPending),   "Pending",   ErrorRed)
                    )
                )
            }

            // KPI strip — 2-col secondary metrics
            item {
                KpiStrip(
                    items = listOf(
                        Triple(dispOrders.toString(),           "Orders",    WarningAmber),
                        Triple(Money.formatRupees(dispAvg),    "Avg order", TextPrimary)
                    )
                )
            }

            // Daily revenue line chart
            item { SectionHeader(text = "Daily revenue") }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceCard)
                        .padding(12.dp)
                ) {
                    Text("DAILY REVENUE", color = TextSecondary, fontSize = 12.sp, letterSpacing = 0.5.sp)
                    androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
                    if (dispDaily.isEmpty()) {
                        Text("No sales data this month", color = TextSecondary, fontSize = 13.sp)
                    } else {
                        SimpleLineChart(
                            values = dispDaily,
                            lineColor = Success,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                        )
                        if (!useDemo) {
                            val daily = report?.daily.orEmpty()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val first = daily.firstOrNull()?.first
                                val last = daily.lastOrNull()?.first
                                if (first != null) Text(DateUtils.formatDayShort(first), color = TextSecondary, fontSize = 13.sp)
                                if (last != null && last != first) Text(DateUtils.formatDayShort(last), color = TextSecondary, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // Collection status donut
            item { SectionHeader(text = "Collection status") }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceCard)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.foundation.Canvas(modifier = Modifier.size(8.dp)) {
                                drawCircle(color = Success)
                            }
                            Text(
                                text = "Collected: ${Money.formatRupees(dispCollected)}",
                                color = TextPrimary, fontSize = 13.sp
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.foundation.Canvas(modifier = Modifier.size(8.dp)) {
                                drawCircle(color = ErrorRed)
                            }
                            Text(
                                text = "Pending: ${Money.formatRupees(dispPending)}",
                                color = TextPrimary, fontSize = 13.sp
                            )
                        }
                    }
                    MultiSegmentDonut(
                        segments = listOf(
                            Success  to dispCollected.toFloat().coerceAtLeast(0.1f),
                            ErrorRed to dispPending.toFloat().coerceAtLeast(0.1f)
                        ),
                        size = 77.dp,
                        strokeWidth = 12.dp
                    )
                }
            }

            // Top customers — flat rows with dividers
            item { SectionHeader(text = "Top customers") }

            if (useDemo && dispCustomers != null) {
                items(dispCustomers) { c ->
                    val isLast = c == dispCustomers.last()
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            WorkerAvatar(name = c.name, size = 34.dp, fontSize = 9)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(c.name, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.W500)
                                Text("${c.orders} orders", color = TextSecondary, fontSize = 12.sp)
                            }
                            Text(c.total, color = Success, fontSize = 14.sp, fontWeight = FontWeight.W600)
                        }
                        if (!isLast) {
                            Box(Modifier.fillMaxWidth().height(0.5.dp).background(OverlayWhite07))
                        }
                    }
                }
            } else {
                val customers = report?.topCustomers.orEmpty()
                if (customers.isEmpty()) {
                    item {
                        Text(
                            "No customers this month",
                            color = TextSecondary, fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                } else {
                    items(customers, key = { it.first }) { (name, amount) ->
                        val isLast = name == customers.last().first
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(name, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.W500)
                                Text(Money.formatRupees(amount.toLong()), color = Success, fontSize = 14.sp, fontWeight = FontWeight.W600)
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
}
