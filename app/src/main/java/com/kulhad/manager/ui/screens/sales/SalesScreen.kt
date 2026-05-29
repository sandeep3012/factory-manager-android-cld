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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.kulhad.manager.data.util.toDisplay
import com.kulhad.manager.domain.model.SaleStatus
import com.kulhad.manager.domain.model.SaleSummary
import com.kulhad.manager.ui.components.AuditInfoCard
import com.kulhad.manager.ui.charts.SimpleLineChart
import com.kulhad.manager.ui.components.BadgeType
import com.kulhad.manager.ui.components.KpiStrip
import com.kulhad.manager.ui.components.KulhadTopBar
import com.kulhad.manager.ui.components.SaleRowItem
import com.kulhad.manager.ui.components.SectionHeader
import com.kulhad.manager.ui.components.StatusBadge
import com.kulhad.manager.ui.components.WorkingDateChip
import com.kulhad.manager.ui.preview.UiDemoData
import com.kulhad.manager.ui.theme.BgDeep
import com.kulhad.manager.ui.theme.ErrorRed
import com.kulhad.manager.ui.theme.InfoBlue
import com.kulhad.manager.ui.theme.OverlayWhite07
import com.kulhad.manager.ui.theme.PrimaryBlue
import com.kulhad.manager.ui.theme.Success
import com.kulhad.manager.ui.theme.SurfaceCard
import com.kulhad.manager.ui.theme.TextPrimary
import com.kulhad.manager.ui.theme.TextSecondary
import com.kulhad.manager.ui.theme.WarningAmber

@Composable
fun SalesScreen(
    onCreateSale: () -> Unit,
    onPendingPayments: () -> Unit,
    onSaleClick: (Long) -> Unit,
    viewModel: SalesViewModel = hiltViewModel()
) {
    val data by viewModel.tabData.collectAsStateWithLifecycle()
    val workingDate by viewModel.workingDate.collectAsStateWithLifecycle()

    // ── Sale detail dialog ────────────────────────────────────────────────────
    var selectedSummary by remember { mutableStateOf<SaleSummary?>(null) }
    selectedSummary?.let { s ->
        SaleDetailDialog(
            summary   = s,
            onDismiss = { selectedSummary = null }
        )
    }

    // Demo overlay
    val useDemo = UiDemoData.SHOW_DEMO && data.weekTotal == 0 && data.recent.isEmpty()
    val dispWeekTotal  = if (useDemo) UiDemoData.salesWeekTotal  else data.weekTotal.toLong()
    val dispOrders     = if (useDemo) UiDemoData.salesOrderCount else data.orderCount
    val dispCollected  = if (useDemo) UiDemoData.salesCollected  else data.collected.toLong()
    val dispPending    = if (useDemo) UiDemoData.salesPending    else data.pending.toLong()
    val dispDaily      = if (useDemo) UiDemoData.salesDaily7d    else data.daily.map { it.toFloat() }

    Column(modifier = Modifier.fillMaxSize().background(BgDeep)) {
        KulhadTopBar(
            title = "Sales / बिक्री",
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
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Working date chip — tap to change the global working date
            item {
                WorkingDateChip(
                    selectedDate = workingDate,
                    onDateSelected = { viewModel.setWorkingDate(it) }
                )
            }

            // Line chart (matches HTML screen 7)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceCard)
                        .padding(12.dp)
                ) {
                    Text("DAILY SALES — THIS WEEK", color = TextSecondary,
                        fontSize = 10.sp, letterSpacing = 0.5.sp)
                    androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
                    SimpleLineChart(
                        values = dispDaily,
                        chartHeight = 82.dp,
                        lineColor = Success
                    )
                }
            }

            // 4-column KPI strip
            item {
                KpiStrip(
                    items = listOf(
                        Triple(Money.formatRupees(dispWeekTotal), "Sales (7d)", TextPrimary),
                        Triple(dispOrders.toString(),             "Orders",     InfoBlue),
                        Triple(Money.formatRupees(dispCollected), "Collected",  Success),
                        Triple(Money.formatRupees(dispPending),   "Pending",    ErrorRed)
                    )
                )
            }

            // Recent sales list (HTML screen 7 — sale rows with icon box)
            item { SectionHeader(text = "Recent sales") }

            if (useDemo) {
                items(UiDemoData.recentSales) { s ->
                    DemoSaleRow(s)
                }
            } else if (data.recent.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                        contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Outlined.Storefront, contentDescription = null,
                                tint = TextSecondary, modifier = Modifier.size(43.dp))
                            Text("No sales recorded yet", color = TextSecondary, fontSize = 14.sp)
                        }
                    }
                }
            } else {
                items(data.recent, key = { it.sale.id }) { s ->
                    RealSaleRow(s) { selectedSummary = s }
                }
            }
        }
    }
}

@Composable
private fun DemoSaleRow(s: UiDemoData.DemoSale) {
    val badgeType = when (s.status) {
        "Paid"    -> BadgeType.SUCCESS
        "Partial" -> BadgeType.WARNING
        else      -> BadgeType.ERROR
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        SaleRowItem(
            customerName = s.customer,
            meta = s.meta,
            amount = s.amount,
            amountColor = when (s.status) {
                "Paid" -> Success; "Partial" -> WarningAmber; else -> ErrorRed
            },
            trailingContent = { StatusBadge(s.status, badgeType) }
        )
        Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(OverlayWhite07))
    }
}

@Composable
fun RealSaleRow(s: SaleSummary, onClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Icon box (HTML screen 7 style)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(7.dp))
                    .background(InfoBlue.copy(alpha = 0.12f))
                    .padding(7.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Storefront, contentDescription = null,
                    tint = InfoBlue, modifier = Modifier.size(17.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = s.sale.customerName, color = TextPrimary,
                    fontSize = 14.sp, fontWeight = FontWeight.W500)
                Text(
                    text = DateUtils.formatDayShort(s.sale.date),
                    color = TextSecondary, fontSize = 13.sp
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = Money.formatRupees(s.sale.totalAmount),
                    color = Success, fontSize = 14.sp, fontWeight = FontWeight.W600
                )
                when (s.status) {
                    SaleStatus.PAID    -> StatusBadge("Paid",    BadgeType.SUCCESS)
                    SaleStatus.PARTIAL -> StatusBadge("Partial", BadgeType.WARNING)
                    SaleStatus.UNPAID  -> StatusBadge("Unpaid",  BadgeType.ERROR)
                }
            }
        }
        Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(OverlayWhite07))
    }
}

// Keep old SaleCard for backward compatibility with PaymentEntryScreen etc.
@Composable
fun SaleCard(s: SaleSummary, onClick: () -> Unit) = RealSaleRow(s, onClick)

// ── Sale detail dialog ────────────────────────────────────────────────────────

/**
 * View-only dialog showing the full detail of a single [summary].
 *
 * Design constraints:
 *  - No edit or delete actions.
 *  - "Close" is the only button. Tap-outside and hardware back also dismiss.
 *  - [AuditInfoCard] renders createdAt as "—" for migrated rows (createdAt == 0L).
 *  - [SaleEntity] has no remark column in the schema; none is shown here.
 */
@Composable
private fun SaleDetailDialog(
    summary: SaleSummary,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = SurfaceCard,
        title = {
            Text(
                text       = "Sale Details",
                color      = TextPrimary,
                fontSize   = 17.sp,
                fontWeight = FontWeight.W600
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

                // ── Customer (hero value) ─────────────────────────────────
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text          = "CUSTOMER",
                        color         = TextSecondary,
                        fontSize      = 10.sp,
                        fontWeight    = FontWeight.W600,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text       = summary.sale.customerName,
                        color      = InfoBlue,
                        fontSize   = 22.sp,
                        fontWeight = FontWeight.W600
                    )
                }

                // ── Amount breakdown (total / paid / pending) ─────────────
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text          = "TOTAL",
                            color         = TextSecondary,
                            fontSize      = 10.sp,
                            letterSpacing = 0.6.sp
                        )
                        Text(
                            text       = Money.formatRupees(summary.sale.totalAmount),
                            color      = TextPrimary,
                            fontSize   = 15.sp,
                            fontWeight = FontWeight.W500
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text          = "PAID",
                            color         = TextSecondary,
                            fontSize      = 10.sp,
                            letterSpacing = 0.6.sp
                        )
                        Text(
                            text       = Money.formatRupees(summary.paid),
                            color      = Success,
                            fontSize   = 15.sp,
                            fontWeight = FontWeight.W500
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text          = "PENDING",
                            color         = TextSecondary,
                            fontSize      = 10.sp,
                            letterSpacing = 0.6.sp
                        )
                        Text(
                            text       = Money.formatRupees(summary.pending),
                            color      = ErrorRed,
                            fontSize   = 15.sp,
                            fontWeight = FontWeight.W500
                        )
                    }
                }

                // ── Status ────────────────────────────────────────────────
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Status:", color = TextSecondary, fontSize = 13.sp)
                    when (summary.status) {
                        SaleStatus.PAID    -> StatusBadge("Paid",    BadgeType.SUCCESS)
                        SaleStatus.PARTIAL -> StatusBadge("Partial", BadgeType.WARNING)
                        SaleStatus.UNPAID  -> StatusBadge("Unpaid",  BadgeType.ERROR)
                    }
                }

                // ── Date ─────────────────────────────────────────────────
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Date:", color = TextSecondary, fontSize = 13.sp)
                    Text(DateUtils.formatDay(summary.sale.date), color = TextPrimary, fontSize = 13.sp)
                }

                // ── Audit trail ───────────────────────────────────────────
                AuditInfoCard(audit = summary.sale.audit.toDisplay())
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = PrimaryBlue, fontSize = 15.sp, fontWeight = FontWeight.W500)
            }
        }
    )
}
