package com.kulhad.manager.ui.screens.sales

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kulhad.manager.ui.components.bottomSheetContentInsets
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kulhad.manager.data.util.Money
import com.kulhad.manager.domain.model.SaleItemDraft
import kotlin.math.roundToInt
import com.kulhad.manager.ui.components.KulhadButton
import com.kulhad.manager.ui.components.KulhadTextField
import com.kulhad.manager.ui.components.KulhadTopBar
import com.kulhad.manager.ui.components.SectionHeader
import com.kulhad.manager.ui.components.SizePillGrid
import com.kulhad.manager.ui.components.WorkingDateChip
import com.kulhad.manager.ui.theme.BgDeep
import com.kulhad.manager.ui.theme.PrimaryBlue
import com.kulhad.manager.ui.theme.Success
import com.kulhad.manager.ui.theme.SurfaceCard
import com.kulhad.manager.ui.theme.TextPrimary
import com.kulhad.manager.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSaleScreen(
    onBack: () -> Unit,
    viewModel: SalesViewModel = hiltViewModel()
) {
    val products by viewModel.products.collectAsStateWithLifecycle()
    val saleError by viewModel.saleError.collectAsStateWithLifecycle()
    val workingDate by viewModel.workingDate.collectAsStateWithLifecycle()
    var customerName by remember { mutableStateOf("") }
    val items = remember { mutableStateListOf<SaleItemDraft>() }
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    val total = items.sumOf { it.total }

    Column(modifier = Modifier.fillMaxSize().background(BgDeep)) {
        KulhadTopBar(title = "Create Sale", onBack = onBack)
        LazyColumn(
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                KulhadTextField(
                    label = "Customer name",
                    value = customerName,
                    onValueChange = { customerName = it }
                )
            }
            item {
                WorkingDateChip(
                    selectedDate = workingDate,
                    onDateSelected = { viewModel.setWorkingDate(it) }
                )
            }
            item { SectionHeader(text = "Items") }
            items(items.toList(), key = { "${it.productId}-${it.pricePerUnit}-${it.quantity}-${items.indexOf(it)}" }) { d ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceCard)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${d.productSize}ml",
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.W500
                        )
                        // Use priceDisplay so decimal prices (e.g. "3.56") show exactly
                        // what the user entered rather than the rounded integer approximation.
                        val displayPrice = d.priceDisplay.toDoubleOrNull()
                            ?.let { Money.formatRupeesDouble(it) }
                            ?: Money.formatRupees(d.pricePerUnit)
                        Text(
                            text = "${d.quantity} × $displayPrice",
                            color = TextSecondary,
                            fontSize = 17.sp
                        )
                    }
                    Text(
                        text = Money.formatRupees(d.total),
                        color = Success,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.W500
                    )
                    IconButton(onClick = { items.remove(d) }) {
                        Icon(Icons.Outlined.Close, contentDescription = "Remove", tint = TextSecondary)
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceCard)
                        .clickable { showSheet = true }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Outlined.AddCircle, contentDescription = null, tint = PrimaryBlue)
                    Text(text = "Add item", color = PrimaryBlue, fontSize = 17.sp)
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "TOTAL", color = TextSecondary, fontSize = 13.sp, letterSpacing = 0.5.sp)
                    Text(
                        text = Money.formatRupees(total),
                        color = Success,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.W600
                    )
                }
            }
            item {
                KulhadButton(
                    text = "Save Sale",
                    enabled = customerName.isNotBlank() && items.isNotEmpty(),
                    onClick = {
                        viewModel.createSale(customerName.trim(), items.toList()) {
                            onBack()
                        }
                    }
                )
            }
        }
    }

    // ── Insufficient-stock error dialog ──────────────────────────────────────
    saleError?.let { errorMessage ->
        AlertDialog(
            onDismissRequest = { viewModel.clearSaleError() },
            title = {
                Text(
                    text = "Cannot Save Sale",
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
                TextButton(onClick = { viewModel.clearSaleError() }) {
                    Text(text = "OK", color = PrimaryBlue, fontSize = 17.sp)
                }
            },
            containerColor = SurfaceCard
        )
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
            containerColor = SurfaceCard,
            windowInsets = WindowInsets(0) // Content handles nav-bar and IME insets
        ) {
            AddItemSheet(
                products = products,
                viewModel = viewModel,
                onAdd = { d ->
                    items.add(d)
                    showSheet = false
                }
            )
        }
    }
}

@Composable
private fun AddItemSheet(
    products: List<com.kulhad.manager.domain.model.Product>,
    viewModel: SalesViewModel,
    onAdd: (SaleItemDraft) -> Unit
) {
    var sizeMl by remember { mutableStateOf<Int?>(null) }
    var qty    by remember { mutableStateOf("") }
    var price  by remember { mutableStateOf("") }

    val selectedId = products.firstOrNull { it.sizeMl == sizeMl }?.id
    val stock by (selectedId?.let { viewModel.observeStockFor(it) }
        ?: kotlinx.coroutines.flow.flowOf(0)).collectAsStateWithLifecycle(0)

    val qtyInt = qty.toIntOrNull() ?: 0

    // Parse as Double so the user can enter values like 3.56, 3.5, or 10.
    // "3." parses to 3.0 (trailing dot is valid mid-input), "." parses to null → 0.0.
    val priceDouble = price.toDoubleOrNull() ?: 0.0

    // Decimal-aware line total — rounded to the nearest integer rupee.
    // Examples: 4 × 3.56 = 14.24 → 14 ; 4 × 3.75 = 15.0 → 15 ; 100 × 3.25 = 325.0 → 325
    val totalRounded: Int = if (qtyInt > 0 && priceDouble > 0.0)
        (qtyInt.toDouble() * priceDouble).roundToInt()
    else 0

    val maxScrollHeight = (LocalConfiguration.current.screenHeightDp * 0.55f).dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .bottomSheetContentInsets()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxScrollHeight),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(text = "Add item", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.W500)
            }
            item {
                SizePillGrid(
                    sizes = products.map { it.sizeMl },
                    selected = sizeMl,
                    onSelect = { sizeMl = it }
                )
            }
            if (sizeMl != null) {
                item {
                    Text(text = "Available stock: $stock", color = TextSecondary, fontSize = 13.sp)
                }
            }
            // Quantity — integers only; no decimal allowed
            item {
                KulhadTextField(
                    label = "Quantity",
                    value = qty,
                    onValueChange = { qty = it.filter { ch -> ch.isDigit() } },
                    keyboardType = KeyboardType.Number
                )
            }
            // Price per unit — decimal allowed (e.g. 3.56, 3.5, 10.00).
            // Filter: digits and at most ONE decimal point.
            item {
                KulhadTextField(
                    label = "Price per unit (₹)",
                    value = price,
                    onValueChange = { input ->
                        val filtered = input.filter { it.isDigit() || it == '.' }
                        // Allow update only if there is at most one decimal point
                        if (filtered.count { it == '.' } <= 1) price = filtered
                    },
                    keyboardType = KeyboardType.Decimal
                )
            }
            // Live calculated amount — updates on every keystroke.
            // Shows the exact rounded integer rupee total the user will be charged.
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text(
                        text = "Calculated Amount",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                    Text(
                        text = Money.formatRupees(totalRounded),
                        color = Success,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.W600
                    )
                }
            }
        }
        KulhadButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 16.dp),
            text = "Add to sale",
            enabled = sizeMl != null && qtyInt > 0 && priceDouble > 0.0,
            onClick = {
                val product = products.firstOrNull { it.sizeMl == sizeMl } ?: return@KulhadButton
                onAdd(
                    SaleItemDraft(
                        productId        = product.id,
                        productSize      = product.sizeMl,
                        quantity         = qtyInt,
                        // Store nearest-integer approximation of the decimal price in the
                        // DB column (INTEGER).  The authoritative sale total comes from
                        // precomputedTotal; the stored per-unit value is only cosmetic.
                        pricePerUnit     = priceDouble.roundToInt().coerceAtLeast(1),
                        // Preserve the raw decimal string for display in the items list.
                        priceDisplay     = price,
                        // The correctly rounded total — this is what SaleRepository sums.
                        precomputedTotal = totalRounded
                    )
                )
            }
        )
    }
}
