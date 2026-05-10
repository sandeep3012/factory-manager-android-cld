package com.kulhad.manager.ui.screens.production

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kulhad.manager.data.util.DateUtils
import com.kulhad.manager.data.util.Money
import com.kulhad.manager.ui.components.KulhadButton
import com.kulhad.manager.ui.components.KulhadTextField
import com.kulhad.manager.ui.components.KulhadTopBar
import com.kulhad.manager.ui.components.SizePillGrid
import com.kulhad.manager.ui.theme.BgDeep
import com.kulhad.manager.ui.theme.ErrorRed
import com.kulhad.manager.ui.theme.PrimaryBlueDark
import com.kulhad.manager.ui.theme.Success
import com.kulhad.manager.ui.theme.SurfaceCard
import com.kulhad.manager.ui.theme.TextPrimary
import com.kulhad.manager.ui.theme.TextSecondary
import com.kulhad.manager.ui.theme.WarningAmber

@Composable
fun AddProductionScreen(
    onBack: () -> Unit,
    viewModel: ProductionViewModel = hiltViewModel()
) {
    val workers by viewModel.activeWorkers.collectAsStateWithLifecycle()
    val products by viewModel.productsWithRates.collectAsStateWithLifecycle()
    var workerId by remember { mutableStateOf<Long?>(null) }
    var sizeMl by remember { mutableStateOf<Int?>(null) }
    var qty by remember { mutableStateOf("") }
    var defective by remember { mutableStateOf("0") }
    val date = DateUtils.todayStart()
    var rate by remember { mutableStateOf(0.0) }

    LaunchedEffect(workers) {
        if (workerId == null) workerId = workers.firstOrNull()?.id
    }

    LaunchedEffect(sizeMl, products) {
        val pid = products.firstOrNull { it.product.sizeMl == sizeMl }?.product?.id
        rate = pid?.let { viewModel.rateFor(it) } ?: 0.0
    }

    val qtyInt = qty.toIntOrNull() ?: 0
    val defInt = defective.toIntOrNull() ?: 0
    val net = (qtyInt - defInt).coerceAtLeast(0)
    val earnings = net * rate

    Column(modifier = Modifier.fillMaxSize().background(BgDeep)) {
        KulhadTopBar(title = "Add Production", onBack = onBack)
        LazyColumn(
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { Text("WORKER", color = TextSecondary, fontSize = 9.sp, letterSpacing = 0.5.sp) }
            items(workers, key = { it.id }) { w ->
                val sel = w.id == workerId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (sel) PrimaryBlueDark else SurfaceCard)
                        .clickable { workerId = w.id }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = w.name,
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = if (sel) FontWeight.W600 else FontWeight.W500,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = w.currentType.name.lowercase().replaceFirstChar { it.uppercase() },
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }
            }
            item { Text("KULHAD SIZE", color = TextSecondary, fontSize = 9.sp, letterSpacing = 0.5.sp) }
            item {
                SizePillGrid(
                    sizes = products.map { it.product.sizeMl },
                    selected = sizeMl,
                    onSelect = { sizeMl = it }
                )
            }
            if (sizeMl != null) {
                item {
                    Text(
                        text = "Rate · ${sizeMl}ml — ${Money.formatRupeesDouble(rate)} / piece",
                        color = WarningAmber,
                        fontSize = 11.sp
                    )
                }
            }
            item {
                KulhadTextField(
                    label = "Quantity produced",
                    value = qty,
                    onValueChange = { qty = it.filter { ch -> ch.isDigit() } },
                    keyboardType = KeyboardType.Number
                )
            }
            item {
                KulhadTextField(
                    label = "Defective quantity",
                    value = defective,
                    onValueChange = { defective = it.filter { ch -> ch.isDigit() } },
                    keyboardType = KeyboardType.Number,
                    helper = "Defective pieces will not be added to stock"
                )
            }
            item {
                Text(
                    text = "Date: ${DateUtils.formatDay(date)}",
                    color = TextSecondary,
                    fontSize = 10.sp
                )
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceCard)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "Stock change", color = TextSecondary, fontSize = 9.sp)
                        Text(text = "+$net", color = Success, fontSize = 14.sp, fontWeight = FontWeight.W600)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "Earnings", color = TextSecondary, fontSize = 9.sp)
                        Text(
                            text = Money.formatRupeesDouble(earnings),
                            color = Success,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.W600
                        )
                    }
                }
            }
            item {
                if (qtyInt > 0 && defInt > qtyInt) {
                    Text(
                        text = "Defective cannot exceed quantity",
                        color = ErrorRed,
                        fontSize = 11.sp
                    )
                }
            }
            item {
                KulhadButton(
                    text = "Save Entry",
                    enabled = workerId != null && sizeMl != null && qtyInt > 0 && defInt <= qtyInt,
                    onClick = {
                        val pid = products.firstOrNull { it.product.sizeMl == sizeMl }?.product?.id
                            ?: return@KulhadButton
                        val wid = workerId ?: return@KulhadButton
                        viewModel.saveEntry(wid, pid, qtyInt, defInt, date) { onBack() }
                    }
                )
            }
        }
    }
}
