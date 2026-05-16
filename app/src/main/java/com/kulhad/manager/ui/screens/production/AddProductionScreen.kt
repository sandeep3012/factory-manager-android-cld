package com.kulhad.manager.ui.screens.production

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.kulhad.manager.ui.components.SectionHeader
import com.kulhad.manager.ui.components.SizePillGrid
import com.kulhad.manager.ui.theme.BgDeep
import com.kulhad.manager.ui.theme.ErrorRed
import com.kulhad.manager.ui.theme.OverlayWhite07
import com.kulhad.manager.ui.theme.PrimaryBlue
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
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Worker selection — flat rows with selection dot
            item { SectionHeader(text = "Worker") }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceCard)
                        .padding(horizontal = 12.dp)
                ) {
                    workers.forEachIndexed { idx, w ->
                        val sel = w.id == workerId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { workerId = w.id }
                                .padding(vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (sel) PrimaryBlue else Color.Transparent)
                            )
                            Text(
                                text = w.name,
                                color = TextPrimary,
                                fontSize = 17.sp,
                                fontWeight = if (sel) FontWeight.W600 else FontWeight.W500,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = w.currentType.name.lowercase().replaceFirstChar { it.uppercase() },
                                color = if (sel) PrimaryBlue else TextSecondary,
                                fontSize = 17.sp
                            )
                        }
                        if (idx < workers.lastIndex) {
                            Box(Modifier.fillMaxWidth().height(0.5.dp).background(OverlayWhite07))
                        }
                    }
                    if (workers.isEmpty()) {
                        Text(
                            "No workers found",
                            color = TextSecondary, fontSize = 13.sp,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                }
            }

            // Kulhad size picker
            item { SectionHeader(text = "Kulhad size") }
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
                        fontSize = 13.sp
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
                    color = TextSecondary, fontSize = 17.sp
                )
            }

            // Preview summary card
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceCard)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Stock change", color = TextSecondary, fontSize = 17.sp, letterSpacing = 0.5.sp)
                        Text(text = "+$net pcs", color = Success, fontSize = 17.sp, fontWeight = FontWeight.W600)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "Earnings", color = TextSecondary, fontSize = 17.sp, letterSpacing = 0.5.sp)
                        Text(
                            text = Money.formatRupeesDouble(earnings),
                            color = Success, fontSize = 17.sp, fontWeight = FontWeight.W600
                        )
                    }
                }
            }

            if (qtyInt > 0 && defInt > qtyInt) {
                item {
                    Text(
                        text = "Defective cannot exceed quantity",
                        color = ErrorRed, fontSize = 13.sp
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
