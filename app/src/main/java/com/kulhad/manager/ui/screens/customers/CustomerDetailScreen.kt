package com.kulhad.manager.ui.screens.customers

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
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kulhad.manager.data.util.DateUtils
import com.kulhad.manager.data.util.Money
import com.kulhad.manager.domain.model.CustomerDetailData
import com.kulhad.manager.domain.model.SaleSummary
import com.kulhad.manager.domain.model.SaleStatus
import com.kulhad.manager.ui.components.BadgeType
import com.kulhad.manager.ui.components.EmptyState
import com.kulhad.manager.ui.components.KulhadTopBar
import com.kulhad.manager.ui.components.SectionHeader
import com.kulhad.manager.ui.components.StatusBadge
import com.kulhad.manager.ui.theme.BgDeep
import com.kulhad.manager.ui.theme.BorderLine
import com.kulhad.manager.ui.theme.ErrorRed
import com.kulhad.manager.ui.theme.PrimaryBlue
import com.kulhad.manager.ui.theme.SurfaceCard
import com.kulhad.manager.ui.theme.TextPrimary
import com.kulhad.manager.ui.theme.TextSecondary
import com.kulhad.manager.ui.theme.WarningAmber

@Composable
fun CustomerDetailScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onSaleClick: (Long) -> Unit,
    viewModel: CustomerDetailViewModel = hiltViewModel()
) {
    val detail by viewModel.detail.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().background(BgDeep)) {
        KulhadTopBar(
            title = detail?.customer?.name ?: "Customer",
            onBack = onBack,
            actions = {
                detail?.customer?.id?.let { customerId ->
                    IconButton(onClick = { onEdit(customerId) }) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Edit", tint = PrimaryBlue)
                    }
                }
            }
        )

        if (detail == null) {
            EmptyState(message = "Loading...")
        } else {
            CustomerDetailContent(detail = detail!!, onSaleClick = onSaleClick)
        }
    }
}

@Composable
private fun CustomerDetailContent(detail: CustomerDetailData, onSaleClick: (Long) -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Customer info card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceCard)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InfoRow(label = "Code", value = detail.customer.customerCode)
                HorizontalDivider(color = BorderLine)
                InfoRow(label = "Type", value = detail.customer.customerType)
                if (detail.customer.mobileNumber.isNotBlank()) {
                    HorizontalDivider(color = BorderLine)
                    InfoRow(label = "Mobile", value = detail.customer.mobileNumber)
                }
                if (detail.customer.address.isNotBlank()) {
                    HorizontalDivider(color = BorderLine)
                    InfoRow(label = "Address", value = detail.customer.address)
                }
            }
        }

        // Statistics card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceCard)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatRow(label = "Total Sales", value = Money.formatRupees(detail.totalSales), valueColor = TextPrimary)
                HorizontalDivider(color = BorderLine)
                StatRow(label = "Total Payments", value = Money.formatRupees(detail.totalPaid), valueColor = TextPrimary)
                HorizontalDivider(color = BorderLine)
                StatRow(
                    label = "Outstanding",
                    value = Money.formatRupees(detail.outstanding),
                    valueColor = if (detail.outstanding > 0) WarningAmber else TextSecondary
                )
                if (detail.lastPurchaseDate != null) {
                    HorizontalDivider(color = BorderLine)
                    StatRow(
                        label = "Last Sale",
                        value = DateUtils.formatDay(detail.lastPurchaseDate),
                        valueColor = TextSecondary
                    )
                }
            }
        }

        // Sales history
        item { SectionHeader(text = "Sales History (${detail.sales.size})") }

        if (detail.sales.isEmpty()) {
            item { EmptyState(message = "No sales yet") }
        } else {
            items(detail.sales, key = { it.sale.id }) { summary ->
                SaleHistoryRow(summary = summary, onClick = { onSaleClick(summary.sale.id) })
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = TextSecondary, fontSize = 14.sp)
        Text(text = value, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.W500)
    }
}

@Composable
private fun StatRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = TextSecondary, fontSize = 14.sp)
        Text(text = value, color = valueColor, fontSize = 15.sp, fontWeight = FontWeight.W500)
    }
}

@Composable
private fun SaleHistoryRow(summary: SaleSummary, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceCard)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = DateUtils.formatDay(summary.sale.date),
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.W500
            )
            Text(
                text = Money.formatRupees(summary.sale.totalAmount),
                color = TextSecondary,
                fontSize = 13.sp
            )
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val (badgeText, badgeType) = when (summary.status) {
                SaleStatus.PAID    -> "Paid"    to BadgeType.SUCCESS
                SaleStatus.PARTIAL -> "Partial" to BadgeType.WARNING
                SaleStatus.UNPAID  -> "Unpaid"  to BadgeType.ERROR
            }
            StatusBadge(text = badgeText, type = badgeType)
            if (summary.pending > 0) {
                Text(
                    text = "₹${summary.pending} due",
                    color = ErrorRed,
                    fontSize = 12.sp
                )
            }
        }
    }
}
