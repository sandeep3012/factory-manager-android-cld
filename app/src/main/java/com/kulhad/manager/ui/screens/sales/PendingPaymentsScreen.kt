package com.kulhad.manager.ui.screens.sales

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MonetizationOn
import androidx.compose.material3.Icon
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
import com.kulhad.manager.ui.components.HeroCard
import com.kulhad.manager.ui.components.KpiStrip
import com.kulhad.manager.ui.components.KulhadTopBar
import com.kulhad.manager.ui.components.StatusBadge
import com.kulhad.manager.ui.preview.UiDemoData
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

    val useDemo = UiDemoData.SHOW_DEMO && pending.isEmpty() && data.pending == 0

    val dispCollected = if (useDemo) UiDemoData.salesCollected else data.collected.toLong()
    val dispPending   = if (useDemo) UiDemoData.salesPending   else data.pending.toLong()
    val totalDue      = if (useDemo) dispPending else pending.sumOf { it.pending }.toLong()

    Column(modifier = Modifier.fillMaxSize().background(BgDeep)) {
        KulhadTopBar(
            title = "Pending Payments • Udhaar",
            onBack = onBack
        )
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Hero card: total pending
            item {
                HeroCard(
                    label = "Total outstanding",
                    value = Money.formatRupees(totalDue),
                    valueColor = ErrorRed,
                    trailingContent = {
                        DonutChart(
                            primaryValue = dispCollected.toFloat().coerceAtLeast(0.1f),
                            secondaryValue = dispPending.toFloat().coerceAtLeast(0.1f),
                            primaryColor = Success,
                            secondaryColor = ErrorRed,
                            size = 56.dp,
                            strokeWidth = 8.dp
                        )
                    }
                )
            }

            // KPI strip
            item {
                KpiStrip(
                    items = listOf(
                        Triple(Money.formatRupees(dispCollected), "Collected", Success),
                        Triple(Money.formatRupees(dispPending),   "Pending",   ErrorRed)
                    )
                )
            }

            // Demo pending sales
            if (useDemo) {
                val demoPending = listOf(
                    Triple("Gupta Enterprises", "9 May · ₹42,000",  0.60f),
                    Triple("Ram Chai Stall",     "9 May · ₹1,800",   0.00f),
                    Triple("Patel & Sons",       "7 May · ₹12,400",  0.45f),
                )
                items(demoPending) { (name, meta, frac) ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceCard)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(name, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.W500)
                                Text(meta, color = TextSecondary, fontSize = 10.sp)
                            }
                            if (frac > 0f) StatusBadge("Partial", BadgeType.WARNING)
                            else StatusBadge("Unpaid", BadgeType.ERROR)
                        }
                        ProgressBar(progress = frac, color = Success)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val paid = if (frac > 0f) "Paid ${(frac * 100).toInt()}%" else "No payments"
                            val pendStr = if (frac > 0f) "Pending ${(100 - (frac * 100).toInt())}%" else "Full amount"
                            Text(paid, color = Success, fontSize = 10.sp)
                            Text(pendStr, color = ErrorRed, fontSize = 10.sp)
                        }
                    }
                }
            } else if (pending.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Outlined.MonetizationOn, contentDescription = null,
                                tint = TextSecondary, modifier = Modifier.size(36.dp)
                            )
                            Text("No pending payments", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                items(pending, key = { it.sale.id }) { s ->
                    val frac = if (s.sale.totalAmount == 0) 0f else s.paid.toFloat() / s.sale.totalAmount
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
                                Text(s.sale.customerName, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.W500)
                                Text(
                                    "${DateUtils.formatDayShort(s.sale.date)} • ${Money.formatRupees(s.sale.totalAmount)}",
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
                            Text("Paid ${Money.formatRupees(s.paid)}", color = Success, fontSize = 10.sp)
                            Text("Pending ${Money.formatRupees(s.pending)}", color = ErrorRed, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}
