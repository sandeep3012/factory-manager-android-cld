package com.kulhad.manager.ui.screens.sales

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
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.kulhad.manager.domain.model.SaleItemDraft
import com.kulhad.manager.ui.components.KulhadButton
import com.kulhad.manager.ui.components.KulhadTextField
import com.kulhad.manager.ui.components.KulhadTopBar
import com.kulhad.manager.ui.components.SectionHeader
import com.kulhad.manager.ui.components.SizePillGrid
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
    var customerName by remember { mutableStateOf("") }
    val date = DateUtils.todayStart()
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
                Text(
                    text = "Date: ${DateUtils.formatDay(date)}",
                    color = TextSecondary,
                    fontSize = 10.sp
                )
            }
            item { SectionHeader(text = "Items") }
            items(items.toList(), key = { "${it.productId}-${it.pricePerUnit}-${it.quantity}-${items.indexOf(it)}" }) { d ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceCard)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${d.productSize}ml",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.W500
                        )
                        Text(
                            text = "${d.quantity} × ${Money.formatRupees(d.pricePerUnit)}",
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                    Text(
                        text = Money.formatRupees(d.total),
                        color = Success,
                        fontSize = 12.sp,
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
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceCard)
                        .clickable { showSheet = true }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Outlined.AddCircle, contentDescription = null, tint = PrimaryBlue)
                    Text(text = "Add item", color = PrimaryBlue, fontSize = 12.sp)
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "TOTAL", color = TextSecondary, fontSize = 9.sp, letterSpacing = 0.5.sp)
                    Text(
                        text = Money.formatRupees(total),
                        color = Success,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.W600
                    )
                }
            }
            item {
                KulhadButton(
                    text = "Save Sale",
                    enabled = customerName.isNotBlank() && items.isNotEmpty(),
                    onClick = {
                        viewModel.createSale(customerName.trim(), date, items.toList()) {
                            onBack()
                        }
                    }
                )
            }
        }
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
            containerColor = SurfaceCard
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
    var qty by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }

    val selectedId = products.firstOrNull { it.sizeMl == sizeMl }?.id
    val stock by (selectedId?.let { viewModel.observeStockFor(it) }
        ?: kotlinx.coroutines.flow.flowOf(0)).collectAsStateWithLifecycle(0)

    val qtyInt = qty.toIntOrNull() ?: 0
    val priceInt = price.toIntOrNull() ?: 0
    val total = qtyInt * priceInt

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(text = "Add item", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.W500)
        SizePillGrid(
            sizes = products.map { it.sizeMl },
            selected = sizeMl,
            onSelect = { sizeMl = it }
        )
        if (sizeMl != null) {
            Text(text = "Available stock: $stock", color = TextSecondary, fontSize = 11.sp)
        }
        KulhadTextField(
            label = "Quantity",
            value = qty,
            onValueChange = { qty = it.filter { ch -> ch.isDigit() } },
            keyboardType = KeyboardType.Number
        )
        KulhadTextField(
            label = "Price per unit (₹)",
            value = price,
            onValueChange = { price = it.filter { ch -> ch.isDigit() } },
            keyboardType = KeyboardType.Number
        )
        Text(
            text = "Item total: ${Money.formatRupees(total)}",
            color = Success,
            fontSize = 12.sp
        )
        KulhadButton(
            text = "Add to sale",
            enabled = sizeMl != null && qtyInt > 0 && priceInt > 0,
            onClick = {
                val product = products.firstOrNull { it.sizeMl == sizeMl } ?: return@KulhadButton
                onAdd(
                    SaleItemDraft(
                        productId = product.id,
                        productSize = product.sizeMl,
                        quantity = qtyInt,
                        pricePerUnit = priceInt
                    )
                )
            }
        )
    }
}
