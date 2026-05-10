package com.kulhad.manager.ui.screens.stock

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
import com.kulhad.manager.ui.charts.ProgressBar
import com.kulhad.manager.ui.charts.SimpleBarChart
import com.kulhad.manager.ui.components.KulhadTopBar
import com.kulhad.manager.ui.components.SectionHeader
import com.kulhad.manager.ui.theme.BgDeep
import com.kulhad.manager.ui.theme.ErrorRed
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

    Column(modifier = Modifier.fillMaxSize().background(BgDeep)) {
        KulhadTopBar(
            title = "Stock / Inventory",
            actions = {
                TextButton(onClick = onAdjust) {
                    Icon(Icons.Outlined.Tune, contentDescription = null, tint = PrimaryBlue)
                    Text(text = "Adjust", color = PrimaryBlue, fontSize = 12.sp,
                        modifier = Modifier.padding(start = 4.dp))
                }
            }
        )
        LazyColumn(
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceCard)
                        .padding(12.dp)
                ) {
                    Text("STOCK BY SIZE", color = TextSecondary, fontSize = 9.sp, letterSpacing = 0.5.sp)
                    SimpleBarChart(
                        values = items.map { it.quantity.toFloat() },
                        labels = items.map { "${it.product.sizeMl}" },
                        chartHeight = 80.dp,
                        perBarColor = { _, v ->
                            val q = v.toInt()
                            when (StockThresholds.classify(q)) {
                                StockThresholds.Level.HEALTHY -> Success
                                StockThresholds.Level.LOW -> WarningAmber
                                StockThresholds.Level.CRITICAL -> ErrorRed
                            }
                        }
                    )
                }
            }
            item { SectionHeader(text = "Sizes") }
            items(items, key = { it.product.id }) { item ->
                val maxRef = items.maxOfOrNull { it.quantity }?.coerceAtLeast(1) ?: 1
                val frac = item.quantity.toFloat() / maxRef
                val color = when (StockThresholds.classify(item.quantity)) {
                    StockThresholds.Level.HEALTHY -> Success
                    StockThresholds.Level.LOW -> WarningAmber
                    StockThresholds.Level.CRITICAL -> ErrorRed
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceCard)
                        .clickable { onLedger(item.product.id) }
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "${item.product.sizeMl}ml",
                            color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.W500)
                        Text(text = item.quantity.toString(), color = color,
                            fontSize = 14.sp, fontWeight = FontWeight.W600)
                    }
                    ProgressBar(progress = frac, color = color)
                }
            }
        }
    }
}
