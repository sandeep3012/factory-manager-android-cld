package com.kulhad.manager.ui.screens.sales

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kulhad.manager.data.util.DateUtils
import com.kulhad.manager.data.util.Money
import com.kulhad.manager.ui.charts.ProgressBar
import com.kulhad.manager.ui.components.KpiStrip
import com.kulhad.manager.ui.components.KulhadButton
import com.kulhad.manager.ui.components.KulhadButtonStyle
import com.kulhad.manager.ui.components.KulhadTextField
import com.kulhad.manager.ui.components.KulhadTopBar
import com.kulhad.manager.ui.components.SectionHeader
import com.kulhad.manager.ui.theme.BgDeep
import com.kulhad.manager.ui.theme.ErrorRed
import com.kulhad.manager.ui.theme.OverlayWhite07
import com.kulhad.manager.ui.theme.Success
import com.kulhad.manager.ui.theme.SurfaceCard
import com.kulhad.manager.ui.theme.TextPrimary
import com.kulhad.manager.ui.theme.TextSecondary

@Composable
fun PaymentEntryScreen(
    saleId: Long,
    onBack: () -> Unit,
    viewModel: SalesViewModel = hiltViewModel()
) {
    val detail by viewModel.observeSaleDetail(saleId).collectAsStateWithLifecycle(null)
    var amount by remember { mutableStateOf("") }
    var remark by remember { mutableStateOf("") }
    val date = DateUtils.todayStart()

    Column(modifier = Modifier.fillMaxSize().background(BgDeep)) {
        KulhadTopBar(
            title = "Add Payment",
            subtitle = detail?.sale?.customerName ?: "Sale",
            onBack = onBack
        )
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Sale summary card
            detail?.let { d ->
                item {
                    val frac = if (d.sale.totalAmount == 0) 0f
                        else d.paid.toFloat() / d.sale.totalAmount
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(SurfaceCard)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("SALE TOTAL", color = TextSecondary, fontSize = 12.sp, letterSpacing = 0.5.sp)
                                Text(
                                    Money.formatRupees(d.sale.totalAmount),
                                    color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.W600
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("PAID", color = TextSecondary, fontSize = 12.sp, letterSpacing = 0.5.sp)
                                Text(
                                    Money.formatRupees(d.paid),
                                    color = Success, fontSize = 17.sp, fontWeight = FontWeight.W600
                                )
                                Text(
                                    "PENDING ${Money.formatRupees(d.pending)}",
                                    color = ErrorRed, fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                        ProgressBar(progress = frac, color = Success)
                    }
                }

                // KPI strip
                item {
                    KpiStrip(
                        items = listOf(
                            Triple(Money.formatRupees(d.sale.totalAmount), "Total",   TextPrimary),
                            Triple(Money.formatRupees(d.paid),            "Paid",    Success),
                            Triple(Money.formatRupees(d.pending),         "Pending", ErrorRed)
                        )
                    )
                }
            }

            item {
                KulhadTextField(
                    label = "Amount (₹)",
                    value = amount,
                    onValueChange = { amount = it.filter { ch -> ch.isDigit() } },
                    keyboardType = KeyboardType.Number
                )
            }
            item {
                KulhadTextField(
                    label = "Remark",
                    value = remark,
                    onValueChange = { remark = it }
                )
            }
            item {
                Text(
                    text = "Date: ${DateUtils.formatDay(date)}",
                    color = TextSecondary, fontSize = 12.sp
                )
            }
            item {
                KulhadButton(
                    text = "Save Payment",
                    style = KulhadButtonStyle.SUCCESS,
                    enabled = (amount.toIntOrNull() ?: 0) > 0,
                    onClick = {
                        val amt = amount.toIntOrNull() ?: return@KulhadButton
                        viewModel.addPayment(saleId, amt, date, remark) {
                            amount = ""
                            remark = ""
                        }
                    }
                )
            }

            // Payment history — flat rows with dividers
            detail?.payments?.let { payments ->
                if (payments.isNotEmpty()) {
                    item { SectionHeader(text = "Payment history") }
                    items(payments, key = { it.id }) { p ->
                        val isLast = p == payments.last()
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = Money.formatRupees(p.amount),
                                        color = Success, fontSize = 16.sp, fontWeight = FontWeight.W500
                                    )
                                    if (p.remark.isNotBlank()) {
                                        Text(text = p.remark, color = TextSecondary, fontSize = 12.sp)
                                    }
                                }
                                Text(
                                    text = DateUtils.formatDayShort(p.date),
                                    color = TextSecondary, fontSize = 12.sp
                                )
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
