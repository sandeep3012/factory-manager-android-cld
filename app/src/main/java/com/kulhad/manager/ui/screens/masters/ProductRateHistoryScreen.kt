package com.kulhad.manager.ui.screens.masters

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kulhad.manager.data.util.DateUtils
import com.kulhad.manager.data.util.Money
import com.kulhad.manager.data.util.toDisplay
import com.kulhad.manager.domain.model.PieceRate
import com.kulhad.manager.ui.components.AuditInfoCard
import com.kulhad.manager.ui.components.BadgeType
import com.kulhad.manager.ui.components.KulhadButton
import com.kulhad.manager.ui.components.KulhadTextField
import com.kulhad.manager.ui.components.KulhadTopBar
import com.kulhad.manager.ui.components.SectionHeader
import com.kulhad.manager.ui.components.StatusBadge
import com.kulhad.manager.ui.theme.BgDeep
import com.kulhad.manager.ui.theme.ErrorRed
import com.kulhad.manager.ui.theme.OverlayWhite07
import com.kulhad.manager.ui.theme.PrimaryBlue
import com.kulhad.manager.ui.theme.Success
import com.kulhad.manager.ui.theme.SurfaceCard
import com.kulhad.manager.ui.theme.TextPrimary
import com.kulhad.manager.ui.theme.TextSecondary
import com.kulhad.manager.ui.theme.TextTertiary
import com.kulhad.manager.ui.theme.WarningAmber

// ── Product Rate History Screen ───────────────────────────────────────────────

/**
 * Displays the complete piece-rate history for a single product and allows:
 *  - Adding a new rate row (inserts with current timestamp as [effectiveFrom])
 *  - Editing an existing rate row in-place (does NOT create a duplicate row)
 *
 * The "current" rate is always the row at index 0 (newest [effectiveFrom]).
 * Changing the rate does NOT affect any [production_entries.rate_snapshot] values —
 * production history and salary calculations are fully protected.
 *
 * Navigation: Dashboard → Masters → Products → Tap product → Edit dialog
 *             → "Rate History →" → this screen.
 */
@Composable
fun ProductRateHistoryScreen(
    productId: Long,
    onBack: () -> Unit,
    viewModel: ProductRateHistoryViewModel = hiltViewModel()
) {
    val product     by viewModel.product.collectAsStateWithLifecycle()
    val rateHistory by viewModel.rateHistory.collectAsStateWithLifecycle()

    var showAddDialog    by remember { mutableStateOf(false) }
    var editTarget       by remember { mutableStateOf<PieceRate?>(null) }
    var snackMessage     by remember { mutableStateOf<String?>(null) }

    // ── Add rate dialog ───────────────────────────────────────────────────────
    if (showAddDialog) {
        RateAddDialog(
            onDismiss    = { showAddDialog = false },
            onSave       = { ratePerPiece ->
                viewModel.addRate(ratePerPiece) { result ->
                    when (result) {
                        RateSaveResult.Success     -> showAddDialog = false
                        RateSaveResult.InvalidRate -> snackMessage = "Rate per piece must be greater than zero."
                    }
                }
            },
            errorMessage = snackMessage.also { snackMessage = null }
        )
    }

    // ── Edit rate dialog ──────────────────────────────────────────────────────
    editTarget?.let { rate ->
        RateEditDialog(
            pieceRate    = rate,
            onDismiss    = { editTarget = null },
            onSave       = { newRate ->
                viewModel.updateRate(rate.id, newRate) { result ->
                    when (result) {
                        RateSaveResult.Success     -> editTarget = null
                        RateSaveResult.InvalidRate -> snackMessage = "Rate per piece must be greater than zero."
                    }
                }
            },
            errorMessage = snackMessage.also { snackMessage = null }
        )
    }

    // ── Screen layout ─────────────────────────────────────────────────────────
    Column(modifier = Modifier.fillMaxSize().background(BgDeep)) {
        KulhadTopBar(
            title    = "Production Rates",
            subtitle = product?.description ?: "",
            onBack   = onBack,
            actions  = {
                IconButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add rate", tint = PrimaryBlue)
                }
            }
        )

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ── Product header card ───────────────────────────────────────────
            product?.let { p ->
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(SurfaceCard)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text       = p.description,
                            color      = TextPrimary,
                            fontSize   = 16.sp,
                            fontWeight = FontWeight.W600
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Text(
                                text     = p.displayLabel,
                                color    = TextSecondary,
                                fontSize = 13.sp
                            )
                            Text("·", color = TextTertiary, fontSize = 13.sp)
                            val current = rateHistory.firstOrNull()?.ratePerPiece ?: 0.0
                            Text(
                                text       = "Current: ${Money.formatRupeesDouble(current)}/pc",
                                color      = Success,
                                fontSize   = 13.sp,
                                fontWeight = FontWeight.W500
                            )
                        }
                    }
                }
            }

            // ── History list ──────────────────────────────────────────────────
            if (rateHistory.isEmpty()) {
                item {
                    Box(
                        modifier         = Modifier.fillMaxWidth().padding(top = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text  = "No rates found. Tap + to add one.",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                item {
                    SectionHeader(text = "Rate History — ${rateHistory.size}")
                }
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(SurfaceCard)
                            .padding(horizontal = 12.dp)
                    ) {
                        rateHistory.forEachIndexed { idx, rate ->
                            val isCurrent = idx == 0
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { editTarget = rate }
                                    .padding(vertical = 12.dp),
                                verticalAlignment     = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // Rate value + current badge
                                    Row(
                                        verticalAlignment     = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text       = Money.formatRupeesDouble(rate.ratePerPiece) + "/pc",
                                            color      = if (isCurrent) Success else TextPrimary,
                                            fontSize   = 15.sp,
                                            fontWeight = FontWeight.W600
                                        )
                                        if (isCurrent) {
                                            StatusBadge("Current", BadgeType.SUCCESS)
                                        }
                                    }
                                    // Effective from date
                                    Text(
                                        text     = "Effective: ${DateUtils.formatDay(rate.effectiveFrom)}",
                                        color    = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                    // Audit info
                                    AuditInfoCard(audit = rate.audit.toDisplay())
                                }
                            }
                            if (idx < rateHistory.lastIndex) {
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

// ── Add Rate Dialog ───────────────────────────────────────────────────────────

/**
 * Dialog for adding a brand-new rate row.
 *
 * Only one field: Rate Per Piece (decimal, > 0).
 * [effectiveFrom] is set to the current wall-clock timestamp by [ProductionRepository.addRate].
 */
@Composable
private fun RateAddDialog(
    onDismiss:    () -> Unit,
    onSave:       (ratePerPiece: Double) -> Unit,
    errorMessage: String?
) {
    var rate       by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    val rateDouble   = rate.toDoubleOrNull() ?: 0.0
    val displayError = errorMessage ?: localError

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = SurfaceCard,
        title = {
            Text(
                text       = "Add Production Rate",
                color      = TextPrimary,
                fontSize   = 17.sp,
                fontWeight = FontWeight.W600
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                KulhadTextField(
                    label         = "Rate Per Piece (₹)",
                    value         = rate,
                    onValueChange = { input ->
                        val filtered = input.filter { it.isDigit() || it == '.' }
                        if (filtered.count { it == '.' } <= 1) rate = filtered
                    },
                    keyboardType  = KeyboardType.Decimal,
                    helper        = "e.g. 1.50 — becomes the new current rate immediately"
                )
                if (displayError != null) {
                    Text(text = displayError, color = ErrorRed, fontSize = 12.sp)
                }
                Text(
                    text     = "This will create a new rate entry effective now.\n" +
                               "Past production entries are not affected.",
                    color    = WarningAmber,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        },
        confirmButton = {
            KulhadButton(
                text    = "Add Rate",
                enabled = rateDouble > 0.0,
                onClick = {
                    localError = null
                    onSave(rateDouble)
                }
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = PrimaryBlue, fontSize = 15.sp, fontWeight = FontWeight.W500)
            }
        }
    )
}

// ── Edit Rate Dialog ──────────────────────────────────────────────────────────

/**
 * Dialog for editing an existing [pieceRate] row in-place.
 *
 * Only the [ratePerPiece] value changes — [effectiveFrom] and the creation audit fields
 * are preserved. [auditUpdatedBy] and [auditUpdatedAt] are stamped on save.
 *
 * [AuditInfoCard] is shown so the user can see the full write history of this row.
 */
@Composable
private fun RateEditDialog(
    pieceRate:    PieceRate,
    onDismiss:    () -> Unit,
    onSave:       (newRatePerPiece: Double) -> Unit,
    errorMessage: String?
) {
    var rate by remember(pieceRate.id) {
        mutableStateOf(
            if (pieceRate.ratePerPiece % 1.0 == 0.0)
                pieceRate.ratePerPiece.toInt().toString()
            else
                pieceRate.ratePerPiece.toBigDecimal().stripTrailingZeros().toPlainString()
        )
    }
    var localError by remember { mutableStateOf<String?>(null) }

    val rateDouble   = rate.toDoubleOrNull() ?: 0.0
    val displayError = errorMessage ?: localError

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = SurfaceCard,
        title = {
            Text(
                text       = "Edit Rate",
                color      = TextPrimary,
                fontSize   = 17.sp,
                fontWeight = FontWeight.W600
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Effective from (read-only)
                Text(
                    text     = "Effective: ${DateUtils.formatDay(pieceRate.effectiveFrom)}",
                    color    = TextSecondary,
                    fontSize = 13.sp
                )
                Box(
                    Modifier.fillMaxWidth().height(0.5.dp).background(OverlayWhite07)
                )

                // Rate field
                KulhadTextField(
                    label         = "Rate Per Piece (₹)",
                    value         = rate,
                    onValueChange = { input ->
                        val filtered = input.filter { it.isDigit() || it == '.' }
                        if (filtered.count { it == '.' } <= 1) rate = filtered
                    },
                    keyboardType  = KeyboardType.Decimal,
                    helper        = "Updates this row in-place; past production entries unaffected"
                )

                if (displayError != null) {
                    Text(text = displayError, color = ErrorRed, fontSize = 12.sp)
                }

                // Audit trail
                AuditInfoCard(audit = pieceRate.audit.toDisplay())
            }
        },
        confirmButton = {
            KulhadButton(
                text    = "Save",
                enabled = rateDouble > 0.0,
                onClick = {
                    localError = null
                    onSave(rateDouble)
                }
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = PrimaryBlue, fontSize = 15.sp, fontWeight = FontWeight.W500)
            }
        }
    )
}
