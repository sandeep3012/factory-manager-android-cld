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
import com.kulhad.manager.data.util.Money
import com.kulhad.manager.ui.charts.MultiSegmentDonut
import com.kulhad.manager.ui.charts.SimpleLineChart
import com.kulhad.manager.ui.components.KulhadTopBar
import com.kulhad.manager.ui.components.SectionHeader
import com.kulhad.manager.ui.components.StatCard
import com.kulhad.manager.ui.theme.BgDeep
import com.kulhad.manager.ui.theme.ErrorRed
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
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // KPI strip
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard(
                        value = Money.formatRupees(report?.totalSales ?: 0),
                        label = "Revenue",
                        valueColor = Success,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        value = Money.formatRupees(report?.collected ?: 0),
                        label = "Collected",
                        valueColor = PrimaryBlue,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        value = Money.formatRupees(report?.pending ?: 0),
                        label = "Pending",
                        valueColor = ErrorRed,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard(
                        value = "${report?.orderCount ?: 0}",
                        label = "Orders",
                        valueColor = WarningAmber,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        value = Money.formatRupees(report?.avgOrderValue ?: 0),
                        label = "Avg order",
                        valueColor = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Daily line chart
            item { SectionHeader(text = "Daily revenue") }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceCard)
                        .padding(12.dp)
                ) {
                    val daily = report?.daily.orEmpty()
                    if (daily.isEmpty()) {
                        Text("No sales data this month", color = TextSecondary, fontSize = 11.sp)
                    } else {
                        val values = daily.map { (_, amt) -> amt.toFloat() }
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
                            // show first and last label only to avoid overlap
                            val first = daily.firstOrNull()?.first
                            val last = daily.lastOrNull()?.first
                            if (first != null) Text(
                                text = DateUtils.formatDayShort(first),
                                color = TextSecondary, fontSize = 9.sp
                            )
                            if (last != null && last != first) Text(
                                text = DateUtils.formatDayShort(last),
                                color = TextSecondary, fontSize = 9.sp
                            )
                        }
                    }
                }
            }

            // Collected vs Pending donut
            item { SectionHeader(text = "Collection status") }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceCard)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.foundation.Canvas(modifier = Modifier.size(8.dp)) {
                                drawCircle(color = Success)
                            }
                            Text(
                                text = "Collected: ${Money.formatRupees(report?.collected ?: 0)}",
                                color = TextPrimary,
                                fontSize = 11.sp
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.foundation.Canvas(modifier = Modifier.size(8.dp)) {
                                drawCircle(color = ErrorRed)
                            }
                            Text(
                                text = "Pending: ${Money.formatRupees(report?.pending ?: 0)}",
                                color = TextPrimary,
                                fontSize = 11.sp
                            )
                        }
                    }
                    MultiSegmentDonut(
                        segments = listOf(
                            Success to (report?.collected ?: 0).toFloat(),
                            ErrorRed to (report?.pending ?: 0).toFloat()
                        ),
                        size = 72.dp,
                        strokeWidth = 10.dp
                    )
                }
            }

            // Top customers
            item { SectionHeader(text = "Top customers") }
            val customers = report?.topCustomers.orEmpty()
            if (customers.isEmpty()) {
                item {
                    Text(
                        text = "No customers this month",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            } else {
                items(customers, key = { it.first }) { (name, amount) ->
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
                            text = name,
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.W500
                        )
                        Text(
                            text = Money.formatRupees(amount),
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
