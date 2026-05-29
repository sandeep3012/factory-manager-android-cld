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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kulhad.manager.data.util.DateUtils
import com.kulhad.manager.data.util.Money
import com.kulhad.manager.data.util.toDisplay
import com.kulhad.manager.domain.model.Payment
import com.kulhad.manager.ui.charts.ProgressBar
import com.kulhad.manager.ui.components.AuditInfoCard
import com.kulhad.manager.ui.components.KpiStrip
import com.kulhad.manager.ui.components.KulhadButton
import com.kulhad.manager.ui.components.KulhadButtonStyle
import com.kulhad.manager.ui.components.KulhadTextField
import com.kulhad.manager.ui.components.KulhadTopBar
import com.kulhad.manager.ui.components.SectionHeader
import com.kulhad.manager.ui.components.WorkingDateChip
import com.kulhad.manager.ui.theme.BgDeep
import com.kulhad.manager.ui.theme.ErrorRed
import com.kulhad.manager.ui.theme.OverlayWhite07
import com.kulhad.manager.ui.theme.PrimaryBlue
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
    val detail       by viewModel.observeSaleDetail(saleId).collectAsStateWithLifecycle(null)
    val paymentError by viewModel.paymentError.collectAsStateWithLifecycle()
    val workingDate  by viewModel.workingDate.collectAsStateWithLifecycle()
    var amount          by remember { mutableStateOf("") }
    var remark          by remember { mutableStateOf("") }
    var selectedPayment by remember { mutableStateOf<Payment?>(null) }

    // ── Overpayment error dialog ─────────────────────────────────────────────
    paymentError?.let { errorMessage ->
        AlertDialog(
            onDismissRequest = { viewModel.clearPaymentError() },
            title = {
                Text(
                    text = "Cannot Collect Payment",
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.W500
                )
            },
            text = {
                Text(
                    text = errorMessage,
                    color = TextSecondary,
                    fontSize = 17.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.clearPaymentError() }) {
                    Text(text = "OK", color = PrimaryBlue, fontSize = 17.sp)
                }
            },
            containerColor = SurfaceCard
        )
    }

    // ── Payment detail dialog (view-only, tap outside / back to close) ───────
    selectedPayment?.let { p ->
        PaymentDetailDialog(
            payment   = p,
            onDismiss = { selectedPayment = null }
        )
    }

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
            // Working date chip — date is read from WorkingDateManager inside addPayment()
            item {
                WorkingDateChip(
                    selectedDate = workingDate,
                    onDateSelected = { viewModel.setWorkingDate(it) }
                )
            }
            item {
                KulhadButton(
                    text = "Save Payment",
                    style = KulhadButtonStyle.SUCCESS,
                    enabled = (amount.toIntOrNull() ?: 0) > 0,
                    onClick = {
                        val amt = amount.toIntOrNull() ?: return@KulhadButton
                        viewModel.addPayment(saleId, amt, remark) {
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
                                    .clickable { selectedPayment = p }
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

// ── Payment detail dialog ─────────────────────────────────────────────────────

/**
 * View-only dialog that surfaces the full details — amount, date, remark, and audit trail —
 * for a single [payment] row.
 *
 * Design constraints:
 *  - No edit or delete actions; [confirmButton] is a single "Close" dismissal.
 *  - Tap outside ([onDismissRequest]) and hardware back both invoke [onDismiss].
 *  - [AuditInfoCard] renders createdAt as "—" when the row was migrated from v1 (createdAt == 0L).
 */
@Composable
private fun PaymentDetailDialog(
    payment: Payment,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = SurfaceCard,
        title = {
            Text(
                text       = "Payment Details",
                color      = TextPrimary,
                fontSize   = 17.sp,
                fontWeight = FontWeight.W600
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

                // ── Amount ───────────────────────────────────────────────────
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text          = "AMOUNT",
                        color         = TextSecondary,
                        fontSize      = 10.sp,
                        fontWeight    = FontWeight.W600,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text       = Money.formatRupees(payment.amount),
                        color      = Success,
                        fontSize   = 22.sp,
                        fontWeight = FontWeight.W600
                    )
                }

                // ── Date ─────────────────────────────────────────────────────
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Date:", color = TextSecondary, fontSize = 13.sp)
                    Text(DateUtils.formatDay(payment.date), color = TextPrimary, fontSize = 13.sp)
                }

                // ── Remark (shown only when non-blank) ────────────────────────
                if (payment.remark.isNotBlank()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Remark:", color = TextSecondary, fontSize = 13.sp)
                        Text(
                            text       = payment.remark,
                            color      = TextPrimary,
                            fontSize   = 13.sp,
                            modifier   = Modifier.weight(1f)
                        )
                    }
                }

                // ── Audit trail ───────────────────────────────────────────────
                AuditInfoCard(audit = payment.audit.toDisplay())
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = PrimaryBlue, fontSize = 15.sp, fontWeight = FontWeight.W500)
            }
        }
    )
}
