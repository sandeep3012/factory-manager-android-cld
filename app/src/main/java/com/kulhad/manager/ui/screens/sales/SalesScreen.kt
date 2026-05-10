package com.kulhad.manager.ui.screens.sales

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.kulhad.manager.domain.model.SaleStatus
import com.kulhad.manager.domain.model.SaleSummary
import com.kulhad.manager.ui.charts.SimpleLineChart
import com.kulhad.manager.ui.components.BadgeType
import com.kulhad.manager.ui.components.EmptyState
import com.kulhad.manager.ui.components.KulhadTopBar
import com.kulhad.manager.ui.components.SectionHeader
import com.kulhad.manager.ui.components.StatCard
import com.kulhad.manager.ui.components.StatusBadge
import com.kulhad.manager.ui.theme.BgDeep
import com.kulhad.manager.ui.theme.ErrorRed
import com.kulhad.manager.ui.theme.PrimaryBlue
import com.kulhad.manager.ui.theme.Success
import com.kulhad.manager.ui.theme.SurfaceCard
import com.kulhad.manager.ui.theme.TextPrimary
import com.kulhad.manager.ui.theme.TextSecondary

@Composable
fun SalesScreen(
    onCreateSale: () -> Unit,
    onPendingPayments: () -> Unit,
    onSaleClick: (Long) -> Unit,
    viewModel: SalesViewModel = hiltViewModel()
) {
    val data by viewModel.tabData.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().background(BgDeep)) {
        KulhadTopBar(
            title = "Sales",
            actions = {
                IconButton(onClick = onPendingPayments) {
                    Icon(Icons.Outlined.Payments, contentDescription = "Pending", tint = TextPrimary)
                }
                IconButton(onClick = onCreateSale) {
                    Icon(Icons.Filled.Add, contentDescription = "New Sale", tint = PrimaryBlue)
                }
            }
        )
        LazyColumn(
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard(
                        value = Money.formatRupees(data.weekTotal),
                        label = "Sales (7d)",
                        valueColor = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        value = data.orderCount.toString(),
                        label = "Orders",
                        valueColor = PrimaryBlue,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard(
                        value = Money.formatRupees(data.collected),
                        label = "Collected",
                        valueColor = Success,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        value = Money.formatRupees(data.pending),
                        label = "Pending",
                        valueColor = ErrorRed,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            item { SectionHeader(text = "7-day sales") }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceCard)
                        .padding(12.dp)
                ) {
                    SimpleLineChart(
                        values = data.daily.map { it.toFloat() },
                        chartHeight = 80.dp,
                        lineColor = Success
                    )
                }
            }
            item { SectionHeader(text = "Recent sales") }
            if (data.recent.isEmpty()) {
                item { EmptyState(message = "No sales recorded yet", icon = Icons.Outlined.Storefront) }
            } else {
                items(data.recent, key = { it.sale.id }) { s ->
                    SaleCard(s) { onSaleClick(s.sale.id) }
                }
            }
        }
    }
}

@Composable
fun SaleCard(s: SaleSummary, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceCard)
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = s.sale.customerName, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.W500)
            Text(
                text = "${DateUtils.formatDayShort(s.sale.date)} • ${Money.formatRupees(s.sale.totalAmount)}",
                color = TextSecondary, fontSize = 10.sp
            )
            if (s.pending > 0) {
                Text(
                    text = "Pending ${Money.formatRupees(s.pending)}",
                    color = ErrorRed,
                    fontSize = 10.sp
                )
            }
        }
        when (s.status) {
            SaleStatus.PAID -> StatusBadge("Paid", BadgeType.SUCCESS)
            SaleStatus.PARTIAL -> StatusBadge("Partial", BadgeType.WARNING)
            SaleStatus.UNPAID -> StatusBadge("Unpaid", BadgeType.ERROR)
        }
    }
}
