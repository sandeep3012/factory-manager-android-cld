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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kulhad.manager.data.util.StockThresholds
import com.kulhad.manager.ui.charts.MultiSegmentDonut
import com.kulhad.manager.ui.charts.ProgressBar
import com.kulhad.manager.ui.components.KpiStrip
import com.kulhad.manager.ui.components.KulhadTopBar
import com.kulhad.manager.ui.components.SectionHeader
import com.kulhad.manager.ui.preview.UiDemoData
import com.kulhad.manager.ui.theme.BgDeep
import com.kulhad.manager.ui.theme.ErrorRed
import com.kulhad.manager.ui.theme.InfoBlue
import com.kulhad.manager.ui.theme.OverlayWhite07
import com.kulhad.manager.ui.theme.PrimaryBlue
import com.kulhad.manager.ui.theme.Success
import com.kulhad.manager.ui.theme.SurfaceCard
import com.kulhad.manager.ui.theme.TextPrimary
import com.kulhad.manager.ui.theme.TextSecondary
import com.kulhad.manager.ui.theme.WarningAmber

@Composable
fun StockScreen(
    onAdjust: () -> Unit,
    onLedger: (Long) -> Unit,
    viewModel: StockViewModel = hiltViewModel()
) {
    val items by viewModel.stockItems.collectAsStateWithLifecycle()

    // Demo overlay when all stock is zero
    val useDemo = UiDemoData.SHOW_DEMO && items.all { it.quantity == 0 }

    data class StockRow(val id: Long, val label: String, val qty: Int, val maxQty: Int)

    val displayItems: List<StockRow> = if (useDemo) {
        val max = UiDemoData.stockItems.maxOf { it.qty }.coerceAtLeast(1)
        UiDemoData.stockItems.mapIndexed { i, d ->
            StockRow(i.toLong(), "${d.sizeMl}ml", d.qty, max)
        }
    } else {
        val max = items.maxOfOrNull { it.quantity }?.coerceAtLeast(1) ?: 1
        items.map { StockRow(it.product.id, "${it.product.sizeMl}ml", it.quantity, max) }
    }

    val totalQty   = displayItems.sumOf { it.qty }
    val healthyQty = displayItems.count { StockThresholds.classify(it.qty) == StockThresholds.Level.HEALTHY }
    val lowQty     = displayItems.count { StockThresholds.classify(it.qty) == StockThresholds.Level.LOW }
    val critQty    = displayItems.count { StockThresholds.classify(it.qty) == StockThresholds.Level.CRITICAL }

    Column(modifier = Modifier.fillMaxSize().background(BgDeep)) {
        KulhadTopBar(
            title = "Stock / Inventory",
            actions = {
                TextButton(onClick = onAdjust) {
                    Icon(Icons.Outlined.Tune, contentDescription = null, tint = PrimaryBlue)
                    Text("Adjust", color = PrimaryBlue, fontSize = 13.sp,
                        modifier = Modifier.padding(start = 4.dp))
                }
            }
        )

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Overview: donut + KPI strip (matches HTML screen 6)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceCard)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    MultiSegmentDonut(
                        segments = listOf(
                            Success     to healthyQty.toFloat().coerceAtLeast(0.1f),
                            WarningAmber to lowQty.toFloat().coerceAtLeast(0.1f),
                            ErrorRed    to critQty.toFloat().coerceAtLeast(0.1f)
                        ),
                        size = 77.dp,
                        strokeWidth = 12.dp
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("STOCK STATUS", color = TextSecondary, fontSize = 14.sp, letterSpacing = 0.5.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            LegendDot(Success,      "$healthyQty Healthy")
                            LegendDot(WarningAmber, "$lowQty Low")
                            LegendDot(ErrorRed,     "$critQty Critical")
                        }
                        Text("Total: $totalQty pieces",
                            color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.W500)
                    }
                }
            }

            // KPI strip
            item {
                KpiStrip(
                    items = listOf(
                        Triple(totalQty.toString(),    "Total pcs",  TextPrimary),
                        Triple(healthyQty.toString(),  "Healthy",    Success),
                        Triple(lowQty.toString(),      "Low",        WarningAmber),
                        Triple(critQty.toString(),     "Critical",   ErrorRed)
                    )
                )
            }

            // Progress bar rows (matches HTML screen 6 — material progress bars)
            item { SectionHeader(text = "By size") }
            items(displayItems, key = { it.id }) { row ->
                val frac = row.qty.toFloat() / row.maxQty
                val color = when (StockThresholds.classify(row.qty)) {
                    StockThresholds.Level.HEALTHY  -> Success
                    StockThresholds.Level.LOW      -> WarningAmber
                    StockThresholds.Level.CRITICAL -> ErrorRed
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (!useDemo) {
                                val realItem = items.find { "${it.product.sizeMl}ml" == row.label }
                                realItem?.let { onLedger(it.product.id) }
                            }
                        }
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = row.label, color = TextPrimary,
                            fontSize = 14.sp, fontWeight = FontWeight.W500)
                        Text(text = "${row.qty} pcs — ${(frac * 100).toInt()}%",
                            color = TextSecondary, fontSize = 14.sp)
                    }
                    ProgressBar(progress = frac, color = color, height = 6.dp)
                }
                Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(OverlayWhite07))
            }
        }
    }
}

@Composable
private fun LegendDot(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.height(8.dp).then(Modifier.padding(0.dp))
            .clip(RoundedCornerShape(2.dp)).background(color).padding(horizontal = 4.dp))
        Text(text = label, color = TextSecondary, fontSize = 13.sp)
    }
}
