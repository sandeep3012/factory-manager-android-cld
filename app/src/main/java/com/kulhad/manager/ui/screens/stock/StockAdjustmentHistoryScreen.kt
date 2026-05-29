package com.kulhad.manager.ui.screens.stock

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
import com.kulhad.manager.data.local.entity.StockChangeType
import com.kulhad.manager.data.util.DateUtils
import com.kulhad.manager.data.util.toDisplay
import com.kulhad.manager.domain.model.StockMovement
import com.kulhad.manager.ui.components.AuditInfoCard
import com.kulhad.manager.ui.components.BadgeType
import com.kulhad.manager.ui.components.KpiStrip
import com.kulhad.manager.ui.components.KulhadButton
import com.kulhad.manager.ui.components.KulhadTextField
import com.kulhad.manager.ui.components.KulhadTopBar
import com.kulhad.manager.ui.components.SectionHeader
import com.kulhad.manager.ui.components.StatusBadge
import com.kulhad.manager.ui.components.WorkingDateChip
import com.kulhad.manager.ui.theme.BgDeep
import com.kulhad.manager.ui.theme.ErrorRed
import com.kulhad.manager.ui.theme.OverlayWhite07
import com.kulhad.manager.ui.theme.PrimaryBlue
import com.kulhad.manager.ui.theme.Success
import com.kulhad.manager.ui.theme.SurfaceCard
import com.kulhad.manager.ui.theme.TextPrimary
import com.kulhad.manager.ui.theme.TextSecondary
import com.kulhad.manager.ui.theme.WarningAmber

// ── Stock Adjustment History Screen ──────────────────────────────────────────

/**
 * Date-based view of all LOSS and ADJUSTMENT stock-ledger rows for the globally selected
 * working date. Reacts automatically when the working date changes — no manual refresh.
 *
 * Driven by [StockViewModel.historyDayAdjustments] which uses [flatMapLatest] over
 * [WorkingDateManager.currentWorkingDate] — stale rows from a previously viewed date
 * cannot bleed through on date changes.
 *
 * Tapping a row opens [AdjustmentEditDialog] which allows editing quantity and remark.
 * Product, adjustment type, and date are shown as non-editable context.
 */
@Composable
fun StockAdjustmentHistoryScreen(
    onBack: () -> Unit,
    viewModel: StockViewModel = hiltViewModel()
) {
    val entries     by viewModel.historyDayAdjustments.collectAsStateWithLifecycle()
    val workingDate by viewModel.workingDate.collectAsStateWithLifecycle()

    var selectedEntry by remember { mutableStateOf<StockMovement?>(null) }

    // ── Edit dialog ────────────────────────────────────────────────────────
    selectedEntry?.let { entry ->
        AdjustmentEditDialog(
            adjustment = entry,
            onDismiss  = { selectedEntry = null },
            onSave     = { newQty, newRemark ->
                viewModel.updateAdjustment(
                    id        = entry.id,
                    type      = entry.type,
                    newQty    = newQty,
                    newRemark = newRemark,
                    onDone    = { selectedEntry = null }
                )
            }
        )
    }

    // ── Derived KPIs ──────────────────────────────────────────────────────
    val totalLoss   = entries
        .filter { it.type == StockChangeType.LOSS }
        .sumOf { kotlin.math.abs(it.quantityChange) }
    val totalAdjust = entries
        .filter { it.type == StockChangeType.ADJUSTMENT }
        .sumOf { it.quantityChange }

    Column(modifier = Modifier.fillMaxSize().background(BgDeep)) {
        KulhadTopBar(title = "Adjustment History", onBack = onBack)

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Working date chip — shared singleton, synced across all screens
            item {
                WorkingDateChip(
                    selectedDate   = workingDate,
                    onDateSelected = { viewModel.setWorkingDate(it) }
                )
            }

            // KPI strip
            item {
                KpiStrip(
                    items = listOf(
                        Triple(entries.size.toString(),   "Entries",     TextPrimary),
                        Triple(totalLoss.toString(),      "Loss (pcs)",  ErrorRed),
                        Triple(
                            if (totalAdjust >= 0) "+$totalAdjust" else totalAdjust.toString(),
                            "Net adjust",
                            if (totalAdjust >= 0) Success else WarningAmber
                        )
                    )
                )
            }

            // ── Entry list ────────────────────────────────────────────────
            if (entries.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text     = "No adjustments for this date",
                            color    = TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                item { SectionHeader(text = "Entries — ${entries.size}") }
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(SurfaceCard)
                            .padding(horizontal = 12.dp)
                    ) {
                        entries.forEachIndexed { idx, entry ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedEntry = entry }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text       = "${entry.productSize}ml",
                                        color      = TextPrimary,
                                        fontSize   = 14.sp,
                                        fontWeight = FontWeight.W500
                                    )
                                    Text(
                                        text     = entry.remark.ifBlank { "—" },
                                        color    = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text     = DateUtils.formatTime(entry.timestamp),
                                        color    = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                                // Type badge
                                when (entry.type) {
                                    StockChangeType.LOSS       ->
                                        StatusBadge("Loss",   BadgeType.ERROR)
                                    StockChangeType.ADJUSTMENT ->
                                        StatusBadge("Adjust", BadgeType.WARNING)
                                    else -> {}
                                }
                                // Quantity with sign
                                Text(
                                    text       = if (entry.quantityChange >= 0)
                                                     "+${entry.quantityChange}"
                                                 else entry.quantityChange.toString(),
                                    color      = if (entry.quantityChange >= 0) Success else ErrorRed,
                                    fontSize   = 15.sp,
                                    fontWeight = FontWeight.W600
                                )
                            }
                            if (idx < entries.lastIndex) {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(0.5.dp)
                                        .background(OverlayWhite07)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Adjustment edit dialog ────────────────────────────────────────────────────

/**
 * Edit dialog for a single LOSS or ADJUSTMENT [adjustment].
 *
 * Non-editable context (product, type, date) is shown as static text.
 * Editable fields: quantity (pre-filled with display value) and remark.
 *
 * Quantity display / save convention:
 *  - LOSS: shown as positive magnitude; [onSave] receives the positive value and the ViewModel
 *    re-applies the negative sign before calling the repository.
 *  - ADJUSTMENT: shown as-is (can be negative to reduce stock); [onSave] receives the signed int.
 *
 * [AuditInfoCard] shows CREATED and LAST UPDATED sections. Since adjustments ARE editable,
 * the "Last Updated" section will populate after the first save.
 *
 * Dismiss: tap-outside, hardware back, or the "Close" button.
 * Save: disabled until qty is a non-zero integer and remark is non-blank.
 */
@Composable
private fun AdjustmentEditDialog(
    adjustment: StockMovement,
    onDismiss: () -> Unit,
    onSave: (qty: Int, remark: String) -> Unit
) {
    // Pre-fill: LOSS → show absolute value; ADJUSTMENT → show signed value
    val initialQty = when (adjustment.type) {
        StockChangeType.LOSS -> kotlin.math.abs(adjustment.quantityChange).toString()
        else                 -> adjustment.quantityChange.toString()
    }
    var qty    by remember(adjustment.id) { mutableStateOf(initialQty) }
    var remark by remember(adjustment.id) { mutableStateOf(adjustment.remark) }

    val qtyInt   = qty.toIntOrNull() ?: 0
    val canSave  = qtyInt != 0 && remark.isNotBlank()

    val qtyHelper = when (adjustment.type) {
        StockChangeType.LOSS       -> "Loss is saved as a negative change automatically"
        StockChangeType.ADJUSTMENT -> "Use negative value to reduce stock, positive to add"
        else                       -> ""
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = SurfaceCard,
        title = {
            Text(
                text       = "Edit Adjustment",
                color      = TextPrimary,
                fontSize   = 17.sp,
                fontWeight = FontWeight.W600
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

                // ── Non-editable context ──────────────────────────────────
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text          = "PRODUCT",
                        color         = TextSecondary,
                        fontSize      = 10.sp,
                        fontWeight    = FontWeight.W600,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text       = "${adjustment.productSize}ml",
                        color      = TextPrimary,
                        fontSize   = 20.sp,
                        fontWeight = FontWeight.W600
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Type badge (non-editable)
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Type:", color = TextSecondary, fontSize = 13.sp)
                        when (adjustment.type) {
                            StockChangeType.LOSS       -> StatusBadge("Loss",   BadgeType.ERROR)
                            StockChangeType.ADJUSTMENT -> StatusBadge("Adjust", BadgeType.WARNING)
                            else                       -> {}
                        }
                    }
                    // Date (non-editable)
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Date:", color = TextSecondary, fontSize = 13.sp)
                        Text(
                            DateUtils.formatDay(adjustment.timestamp),
                            color = TextPrimary, fontSize = 13.sp
                        )
                    }
                }

                // ── Editable fields ───────────────────────────────────────
                KulhadTextField(
                    label         = "Quantity",
                    value         = qty,
                    onValueChange = {
                        val filtered = if (adjustment.type == StockChangeType.ADJUSTMENT) {
                            it.filterIndexed { i, c -> c.isDigit() || (i == 0 && c == '-') }
                        } else {
                            it.filter { ch -> ch.isDigit() }
                        }
                        qty = filtered
                    },
                    keyboardType = KeyboardType.Number,
                    helper       = qtyHelper
                )

                KulhadTextField(
                    label         = "Reason / Remark",
                    value         = remark,
                    onValueChange = { remark = it }
                )

                // ── Audit trail ───────────────────────────────────────────
                AuditInfoCard(audit = adjustment.audit.toDisplay())
            }
        },
        confirmButton = {
            KulhadButton(
                text    = "Save",
                enabled = canSave,
                onClick = { onSave(qtyInt, remark) }
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = PrimaryBlue, fontSize = 15.sp, fontWeight = FontWeight.W500)
            }
        }
    )
}
