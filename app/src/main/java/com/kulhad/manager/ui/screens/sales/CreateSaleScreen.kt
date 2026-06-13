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
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.kulhad.manager.domain.model.Customer
import com.kulhad.manager.domain.model.SaleItemDraft
import kotlin.math.roundToInt
import com.kulhad.manager.ui.components.KulhadButton
import com.kulhad.manager.ui.components.KulhadTextField
import com.kulhad.manager.ui.components.KulhadTopBar
import com.kulhad.manager.ui.components.SectionHeader
import com.kulhad.manager.ui.components.SizePillGrid
import com.kulhad.manager.ui.components.WorkingDateChip
import com.kulhad.manager.ui.screens.customers.CustomerViewModel
import com.kulhad.manager.ui.theme.BgDeep
import com.kulhad.manager.ui.theme.BorderLine
import com.kulhad.manager.ui.theme.PrimaryBlue
import com.kulhad.manager.ui.theme.Success
import com.kulhad.manager.ui.theme.SurfaceCard
import com.kulhad.manager.ui.theme.TextPrimary
import com.kulhad.manager.ui.theme.TextSecondary
import com.kulhad.manager.ui.theme.TextTertiary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSaleScreen(
    onBack: () -> Unit,
    viewModel: SalesViewModel = hiltViewModel(),
    customerViewModel: CustomerViewModel = hiltViewModel()
) {
    val products by viewModel.products.collectAsStateWithLifecycle()
    val saleError by viewModel.saleError.collectAsStateWithLifecycle()
    val workingDate by viewModel.workingDate.collectAsStateWithLifecycle()
    val activeCustomers by customerViewModel.activeCustomers.collectAsStateWithLifecycle()
    val lastCreatedId by customerViewModel.lastCreatedId.collectAsStateWithLifecycle()

    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    val items = remember { mutableStateListOf<SaleItemDraft>() }
    var showItemSheet by remember { mutableStateOf(false) }
    var showCustomerSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    // Auto-select a customer that was just quick-added via the picker sheet
    LaunchedEffect(lastCreatedId, activeCustomers) {
        val newId = lastCreatedId ?: return@LaunchedEffect
        val newCustomer = activeCustomers.firstOrNull { it.id == newId }
        if (newCustomer != null) {
            selectedCustomer = newCustomer
            customerViewModel.clearLastCreatedId()
        }
    }

    val total = items.sumOf { it.total }

    Column(modifier = Modifier.fillMaxSize().background(BgDeep)) {
        KulhadTopBar(title = "Create Sale", onBack = onBack)
        LazyColumn(
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ── Customer picker ──────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceCard)
                        .clickable { showCustomerSheet = true }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Outlined.Person, contentDescription = null, tint = if (selectedCustomer != null) PrimaryBlue else TextTertiary)
                    Column(modifier = Modifier.weight(1f)) {
                        if (selectedCustomer != null) {
                            Text(text = selectedCustomer!!.name, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.W500)
                            Text(
                                text = "${selectedCustomer!!.customerCode} · ${selectedCustomer!!.customerType}",
                                color = TextTertiary,
                                fontSize = 12.sp
                            )
                            if (selectedCustomer!!.mobileNumber.isNotBlank()) {
                                Text(text = selectedCustomer!!.mobileNumber, color = TextSecondary, fontSize = 13.sp)
                            }
                        } else {
                            Text(text = "Select customer", color = TextTertiary, fontSize = 16.sp)
                        }
                    }
                    Icon(Icons.Outlined.KeyboardArrowRight, contentDescription = null, tint = TextTertiary)
                }
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
                        .clickable { showItemSheet = true }
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
                    enabled = selectedCustomer != null && items.isNotEmpty(),
                    onClick = {
                        viewModel.createSale(selectedCustomer!!.id, items.toList()) {
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

    if (showItemSheet) {
        ModalBottomSheet(
            onDismissRequest = { showItemSheet = false },
            sheetState = sheetState,
            containerColor = SurfaceCard,
            windowInsets = WindowInsets(0)
        ) {
            AddItemSheet(
                products = products,
                viewModel = viewModel,
                onAdd = { d ->
                    items.add(d)
                    showItemSheet = false
                }
            )
        }
    }

    if (showCustomerSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCustomerSheet = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = SurfaceCard,
            windowInsets = WindowInsets(0)
        ) {
            CustomerPickerSheet(
                customers = activeCustomers,
                onSelect = { customer ->
                    selectedCustomer = customer
                    showCustomerSheet = false
                }
            )
        }
    }
}

@Composable
private fun CustomerPickerSheet(
    customers: List<Customer>,
    onSelect: (Customer) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val maxScrollHeight = (LocalConfiguration.current.screenHeightDp * 0.65f).dp
    val filtered = remember(query, customers) {
        if (query.isBlank()) customers
        else customers.filter {
            it.name.contains(query, ignoreCase = true) ||
                it.mobileNumber.contains(query, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .bottomSheetContentInsets()
    ) {
        Text(
            text = "Select Customer",
            color = TextPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.W500,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp)
        )
        KulhadTextField(
            label = "Search",
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxScrollHeight),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(filtered, key = { it.id }) { customer ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSelect(customer) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = customer.name, color = TextPrimary, fontSize = 16.sp)
                        Text(
                            text = "${customer.customerCode} · ${customer.customerType}",
                            color = TextTertiary,
                            fontSize = 12.sp
                        )
                        if (customer.mobileNumber.isNotBlank()) {
                            Text(text = customer.mobileNumber, color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                }
                HorizontalDivider(color = BorderLine.copy(alpha = 0.4f))
            }
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
                    sizes    = products.map { it.sizeMl },
                    labels   = products.associate { it.sizeMl to it.displayLabel },
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
