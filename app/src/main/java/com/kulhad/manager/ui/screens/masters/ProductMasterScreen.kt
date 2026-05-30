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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.kulhad.manager.data.util.Money
import com.kulhad.manager.data.util.toDisplay
import com.kulhad.manager.domain.model.ProductWithRate
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

// ── Product Master Screen ─────────────────────────────────────────────────────

/**
 * Full CRUD screen for Kulhad product sizes.
 *
 * Shows ALL products (active + inactive) with their current piece rate.
 * Tapping a row opens [ProductEditDialog].
 * The "+" icon in the top bar opens [ProductAddDialog].
 *
 * Delete is intentionally NOT offered — historical transactions reference products
 * by ID and must never break.  Only active/inactive toggle is allowed.
 */
@Composable
fun ProductMasterScreen(
    onBack: () -> Unit,
    viewModel: ProductMasterViewModel = hiltViewModel()
) {
    val products  by viewModel.allProductsWithRates.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var editTarget    by remember { mutableStateOf<ProductWithRate?>(null) }
    var snackMessage  by remember { mutableStateOf<String?>(null) }

    // ── Add dialog ────────────────────────────────────────────────────────────
    if (showAddDialog) {
        ProductAddDialog(
            onDismiss = { showAddDialog = false },
            onSave    = { name, label, order, rate ->
                viewModel.addProduct(name, label, order, rate) { result ->
                    when (result) {
                        ProductSaveResult.Success               -> showAddDialog = false
                        ProductSaveResult.EmptyName             -> snackMessage = "Product name cannot be empty."
                        ProductSaveResult.EmptyLabel            -> snackMessage = "Size label cannot be empty."
                        ProductSaveResult.InvalidRate           -> snackMessage = "Rate per piece must be greater than zero."
                        is ProductSaveResult.DuplicateLabel     -> snackMessage = "\"${result.label}\" already exists."
                    }
                }
            },
            errorMessage = snackMessage.also { snackMessage = null }
        )
    }

    // ── Edit dialog ───────────────────────────────────────────────────────────
    editTarget?.let { pw ->
        ProductEditDialog(
            productWithRate = pw,
            onDismiss       = { editTarget = null },
            onSave          = { name, label, order, isActive, rate ->
                viewModel.updateProduct(pw.product.id, name, label, order, isActive, rate) { result ->
                    when (result) {
                        ProductSaveResult.Success               -> editTarget = null
                        ProductSaveResult.EmptyName             -> snackMessage = "Product name cannot be empty."
                        ProductSaveResult.EmptyLabel            -> snackMessage = "Size label cannot be empty."
                        ProductSaveResult.InvalidRate           -> snackMessage = "Rate per piece must be greater than zero."
                        is ProductSaveResult.DuplicateLabel     -> snackMessage = "\"${result.label}\" already exists."
                    }
                }
            },
            errorMessage = snackMessage.also { snackMessage = null }
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(BgDeep)) {
        KulhadTopBar(
            title = "Product Master",
            onBack = onBack,
            actions = {
                IconButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add product", tint = PrimaryBlue)
                }
            }
        )

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (products.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No products yet. Tap + to add one.",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                item { SectionHeader(text = "Products — ${products.size}") }
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(SurfaceCard)
                            .padding(horizontal = 12.dp)
                    ) {
                        products.forEachIndexed { idx, pw ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { editTarget = pw }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Display order chip
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(BgDeep),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text     = "${pw.product.displayOrder}",
                                        color    = TextTertiary,
                                        fontSize = 11.sp
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text       = pw.product.description,
                                        color      = TextPrimary,
                                        fontSize   = 14.sp,
                                        fontWeight = FontWeight.W500
                                    )
                                    // Label + current rate on the subtitle line
                                    Text(
                                        text     = "${pw.product.displayLabel}  ·  ${Money.formatRupeesDouble(pw.ratePerPiece)}/pc",
                                        color    = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                                if (pw.product.isActive) {
                                    StatusBadge("Active", BadgeType.SUCCESS)
                                } else {
                                    StatusBadge("Inactive", BadgeType.ERROR)
                                }
                            }
                            if (idx < products.lastIndex) {
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

// ── Add Product Dialog ────────────────────────────────────────────────────────

/**
 * Dialog for creating a new product.
 *
 * Fields: Name, Size Label, Display Order, Rate Per Piece.
 * Save is disabled until name, label are non-blank AND rate parses to a value > 0.
 */
@Composable
private fun ProductAddDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, label: String, order: Int, rate: Double) -> Unit,
    errorMessage: String?
) {
    var name  by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }
    var order by remember { mutableStateOf("") }
    var rate  by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    val rateDouble   = rate.toDoubleOrNull() ?: 0.0
    val displayError = errorMessage ?: localError

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = SurfaceCard,
        title = {
            Text(
                text       = "Add Product",
                color      = TextPrimary,
                fontSize   = 17.sp,
                fontWeight = FontWeight.W600
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                KulhadTextField(
                    label         = "Product Name",
                    value         = name,
                    onValueChange = { name = it }
                )
                KulhadTextField(
                    label         = "Size Label (e.g. 80ml, Half Litre)",
                    value         = label,
                    onValueChange = { label = it }
                )
                KulhadTextField(
                    label         = "Display Order",
                    value         = order,
                    onValueChange = { order = it.filter { ch -> ch.isDigit() } },
                    keyboardType  = KeyboardType.Number,
                    helper        = "Lower number = shown first in pickers"
                )
                KulhadTextField(
                    label         = "Rate Per Piece (₹)",
                    value         = rate,
                    onValueChange = { input ->
                        val filtered = input.filter { it.isDigit() || it == '.' }
                        if (filtered.count { it == '.' } <= 1) rate = filtered
                    },
                    keyboardType  = KeyboardType.Decimal,
                    helper        = "e.g. 1.20"
                )
                if (displayError != null) {
                    Text(text = displayError, color = ErrorRed, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            KulhadButton(
                text    = "Add",
                enabled = name.isNotBlank() && label.isNotBlank() && rateDouble > 0.0,
                onClick = {
                    localError = null
                    onSave(name, label, order.toIntOrNull() ?: 0, rateDouble)
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

// ── Edit Product Dialog ───────────────────────────────────────────────────────

/**
 * Edit dialog for a single [productWithRate].
 *
 * Editable: Name, Size Label, Display Order, Rate Per Piece, Active status.
 * Non-editable: product ID, internal sizeMl, creation date.
 *
 * Rate history is append-only — if the rate changes, a new [PieceRateEntity] is inserted.
 * Existing [production_entries.rate_snapshot] values are never affected.
 *
 * Delete is intentionally absent — only deactivation is permitted.
 * [AuditInfoCard] surfaces the full audit trail.
 */
@Composable
private fun ProductEditDialog(
    productWithRate: ProductWithRate,
    onDismiss: () -> Unit,
    onSave: (name: String, label: String, order: Int, isActive: Boolean, rate: Double) -> Unit,
    errorMessage: String?
) {
    val product = productWithRate.product
    var name     by remember(product.id) { mutableStateOf(product.description) }
    var label    by remember(product.id) { mutableStateOf(product.displayLabel) }
    var order    by remember(product.id) { mutableStateOf(product.displayOrder.toString()) }
    var isActive by remember(product.id) { mutableStateOf(product.isActive) }
    // Pre-fill rate: show the current value formatted cleanly (no trailing ".0" for whole numbers)
    var rate     by remember(product.id) {
        mutableStateOf(
            if (productWithRate.ratePerPiece % 1.0 == 0.0)
                productWithRate.ratePerPiece.toInt().toString()
            else
                productWithRate.ratePerPiece.toBigDecimal().stripTrailingZeros().toPlainString()
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
                text       = "Edit Product",
                color      = TextPrimary,
                fontSize   = 17.sp,
                fontWeight = FontWeight.W600
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

                // ── Editable fields ───────────────────────────────────────────
                KulhadTextField(
                    label         = "Product Name",
                    value         = name,
                    onValueChange = { name = it }
                )
                KulhadTextField(
                    label         = "Size Label",
                    value         = label,
                    onValueChange = { label = it }
                )
                KulhadTextField(
                    label         = "Display Order",
                    value         = order,
                    onValueChange = { order = it.filter { ch -> ch.isDigit() } },
                    keyboardType  = KeyboardType.Number,
                    helper        = "Lower number = shown first in pickers"
                )
                KulhadTextField(
                    label         = "Rate Per Piece (₹)",
                    value         = rate,
                    onValueChange = { input ->
                        val filtered = input.filter { it.isDigit() || it == '.' }
                        if (filtered.count { it == '.' } <= 1) rate = filtered
                    },
                    keyboardType  = KeyboardType.Decimal,
                    helper        = "Changing rate adds a new history row; past entries unaffected"
                )

                // ── Active toggle ─────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text       = if (isActive) "Active" else "Inactive",
                            color      = if (isActive) Success else TextSecondary,
                            fontSize   = 14.sp,
                            fontWeight = FontWeight.W500
                        )
                        Text(
                            text  = if (isActive)
                                        "Visible in production, sales, stock screens"
                                    else
                                        "Hidden from entry screens; history unaffected",
                            color = TextTertiary,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked         = isActive,
                        onCheckedChange = { isActive = it },
                        colors          = SwitchDefaults.colors(
                            checkedThumbColor   = Success,
                            checkedTrackColor   = Success.copy(alpha = 0.3f),
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = OverlayWhite07
                        )
                    )
                }

                // ── Error message ─────────────────────────────────────────────
                if (displayError != null) {
                    Text(text = displayError, color = ErrorRed, fontSize = 12.sp)
                }

                // ── Audit trail ───────────────────────────────────────────────
                AuditInfoCard(audit = product.audit.toDisplay())
            }
        },
        confirmButton = {
            KulhadButton(
                text    = "Save",
                enabled = name.isNotBlank() && label.isNotBlank() && rateDouble > 0.0,
                onClick = {
                    localError = null
                    onSave(name, label, order.toIntOrNull() ?: 0, isActive, rateDouble)
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
