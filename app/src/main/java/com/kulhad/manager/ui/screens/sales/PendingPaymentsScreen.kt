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
import androidx.compose.material.icons.outlined.MonetizationOn
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
import com.kulhad.manager.ui.charts.DonutChart
import com.kulhad.manager.ui.charts.ProgressBar
import com.kulhad.manager.ui.components.BadgeType
import com.kulhad.manager.ui.components.EmptyState
import com.kulhad.manager.ui.components.KulhadTopBar
import com.kulhad.manager.ui.components.StatusBadge
import com.kulhad.manager.ui.theme.BgDeep
import com.kulhad.manager.ui.theme.ErrorRed
import com.kulhad.manager.ui.theme.Success
import com.kulhad.manager.ui.theme.SurfaceCard
import com.kulhad.manager.ui.theme.TextPrimary
import com.kulhad.manager.ui.theme.TextSecondary

@Composable
fun PendingPaymentsScreen(
    onBack: () -> Unit,
    onSaleClick: (Long) -> Unit,
    viewModel: SalesViewModel = hiltViewModel()
) {
    val pending by viewModel.pendingSummaries.collectAsStateWithLifecycle()
    val data by viewModel.tabData.collectAsStateWithLifecycle()
    val totalDue = pending.sumOf { it.pending }

    Column(modifier = Modifier.fillMaxSize().background(BgDeep)) {
        KulhadTopBar(
            title = "Pending Payments • Udhaar",
            subtitle = "Total due ${Money.formatRupees(totalDue)}",
            onBack = onBack
        )
        LazyColumn(
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceCard)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("COLLECTED", color = TextSecondary, fontSize = 9.sp, letterSpacing = 0.5.sp)
                        Text(Money.formatRupees(data.collected), color = Success,
                            fontSize = 16.sp, fontWeight = FontWeight.W600)
                        Text("PENDING", color = TextSecondary, fontSize = 9.sp,
                            letterSpacing = 0.5.sp, modifier = Modifier.padding(top = 8.dp))
                        Text(Money.formatRupees(data.pending), color = ErrorRed,
                            fontSize = 16.sp, fontWeight = FontWeight.W600)
                    }
                    DonutChart(
                        primaryValue = data.collected.toFloat(),
                        secondaryValue = data.pending.toFloat(),
                        size = 64.dp,
                        strokeWidth = 10.dp
                    )
                }
            }
            if (pending.isEmpty()) {
                item { EmptyState(message = "No pending payments", icon = Icons.Outlined.MonetizationOn) }
            } else {
                items(pending, key = { it.sale.id }) { s ->
                    val frac =
                        if (s.sale.totalAmount == 0) 0f else s.paid.toFloat() / s.sale.totalAmount
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceCard)
                            .clickable { onSaleClick(s.sale.id) }
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = s.sale.customerName, color = TextPrimary,
                                    fontSize = 13.sp, fontWeight = FontWeight.W500)
                                Text(
                                    text = "${DateUtils.formatDayShort(s.sale.date)} • ${Money.formatRupees(s.sale.totalAmount)}",
                                    color = TextSecondary, fontSize = 10.sp
                                )
                            }
                            when (s.status) {
                                SaleStatus.PARTIAL -> StatusBadge("Partial", BadgeType.WARNING)
                                else -> StatusBadge("Unpaid", BadgeType.ERROR)
                            }
                        }
                        ProgressBar(progress = frac, color = Success)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Paid ${Money.formatRupees(s.paid)}",
                                color = Success, fontSize = 10.sp
                            )
                            Text(
                                text = "Pending ${Money.formatRupees(s.pending)}",
                                color = ErrorRed, fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
