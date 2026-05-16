package com.kulhad.manager.ui.screens.stock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kulhad.manager.data.local.entity.StockChangeType
import com.kulhad.manager.data.util.DateUtils
import com.kulhad.manager.ui.components.KulhadButton
import com.kulhad.manager.ui.components.KulhadTextField
import com.kulhad.manager.ui.components.KulhadTopBar
import com.kulhad.manager.ui.components.SectionHeader
import com.kulhad.manager.ui.components.SegmentedControl
import com.kulhad.manager.ui.components.SizePillGrid
import com.kulhad.manager.ui.theme.BgDeep
import com.kulhad.manager.ui.theme.InfoBlue
import com.kulhad.manager.ui.theme.SurfaceCard
import com.kulhad.manager.ui.theme.TextPrimary
import com.kulhad.manager.ui.theme.TextSecondary

@Composable
fun StockAdjustmentScreen(
    onBack: () -> Unit,
    viewModel: StockViewModel = hiltViewModel()
) {
    val products by viewModel.products.collectAsStateWithLifecycle()
    var typeStr by remember { mutableStateOf("Loss") }
    var sizeMl by remember { mutableStateOf<Int?>(null) }
    var qty by remember { mutableStateOf("") }
    var remark by remember { mutableStateOf("") }

    val productId = products.firstOrNull { it.sizeMl == sizeMl }?.id
    val currentStock by (productId?.let { viewModel.observeStockFor(it) }
        ?: kotlinx.coroutines.flow.flowOf(0)).collectAsStateWithLifecycle(0)

    val type = if (typeStr == "Loss") StockChangeType.LOSS else StockChangeType.ADJUSTMENT

    Column(modifier = Modifier.fillMaxSize().background(BgDeep)) {
        KulhadTopBar(title = "Stock Adjustment", onBack = onBack)
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                SegmentedControl(
                    options = listOf("Loss", "Adjustment"),
                    selected = typeStr,
                    onSelect = { typeStr = it }
                )
            }
            item { SectionHeader(text = "Product size") }
            item {
                SizePillGrid(
                    sizes = products.map { it.sizeMl },
                    selected = sizeMl,
                    onSelect = { sizeMl = it }
                )
            }
            if (sizeMl != null) {
                item {
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceCard)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Current stock", color = TextSecondary, fontSize = 12.sp)
                        Text("$currentStock pcs", color = InfoBlue, fontSize = 13.sp)
                    }
                }
            }
            item {
                val helper = if (type == StockChangeType.ADJUSTMENT)
                    "Use a negative value (e.g. −50) to reduce stock, positive to add"
                else
                    "Loss is automatically applied as a negative change"
                KulhadTextField(
                    label = "Quantity",
                    value = qty,
                    onValueChange = {
                        val filtered = if (type == StockChangeType.ADJUSTMENT) {
                            it.filterIndexed { idx, c -> c.isDigit() || (idx == 0 && c == '-') }
                        } else {
                            it.filter { ch -> ch.isDigit() }
                        }
                        qty = filtered
                    },
                    keyboardType = KeyboardType.Number,
                    helper = helper
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
                    text = "Date: ${DateUtils.formatDay(System.currentTimeMillis())}",
                    color = TextSecondary, fontSize = 12.sp
                )
            }
            item {
                KulhadButton(
                    text = "Save Adjustment",
                    enabled = productId != null && remark.isNotBlank() && (qty.toIntOrNull() ?: 0) != 0,
                    onClick = {
                        val q = qty.toIntOrNull() ?: return@KulhadButton
                        val pid = productId ?: return@KulhadButton
                        viewModel.saveAdjustment(pid, type, q, remark) { onBack() }
                    }
                )
            }
        }
    }
}
