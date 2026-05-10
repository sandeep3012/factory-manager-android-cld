package com.kulhad.manager.ui.screens.stock

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HistoryToggleOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kulhad.manager.data.local.entity.StockChangeType
import com.kulhad.manager.data.util.DateUtils
import com.kulhad.manager.ui.charts.SimpleLineChart
import com.kulhad.manager.ui.components.BadgeType
import com.kulhad.manager.ui.components.KpiStrip
import com.kulhad.manager.ui.components.KulhadTopBar
import com.kulhad.manager.ui.components.StatusBadge
import com.kulhad.manager.ui.theme.BgDeep
import com.kulhad.manager.ui.theme.ErrorRed
import com.kulhad.manager.ui.theme.InfoBlue
import com.kulhad.manager.ui.theme.OverlayWhite07
import com.kulhad.manager.ui.theme.Success
import com.kulhad.manager.ui.theme.SurfaceCard
import com.kulhad.manager.ui.theme.TextPrimary
import com.kulhad.manager.ui.theme.TextSecondary

@Composable
fun StockLedgerScreen(
    productId: Long,
    onBack: () -> Unit,
    viewModel: StockViewModel = hiltViewModel()
) {
    val ledger by viewModel.observeLedger(productId).collectAsStateWithLifecycle(emptyList())
    val current by viewModel.observeStockFor(productId).collectAsStateWithLifecycle(0)
    val balance by viewModel.runningBalance.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val product = products.firstOrNull { it.id == productId }

    LaunchedEffect(productId, ledger.size) {
        viewModel.loadRunningBalance(productId)
    }

    val totalIn  = ledger.filter { it.quantityChange > 0 }.sumOf { it.quantityChange }
    val totalOut = ledger.filter { it.quantityChange < 0 }.sumOf { kotlin.math.abs(it.quantityChange) }

    Column(modifier = Modifier.fillMaxSize().background(BgDeep)) {
        KulhadTopBar(
            title = "Ledger — ${product?.sizeMl ?: ""}ml",
            subtitle = "Current stock: $current pcs",
            onBack = onBack
        )
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Balance line chart
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceCard)
                        .padding(12.dp)
                ) {
                    Text("STOCK LEVEL — LAST 7 MOVEMENTS", color = TextSecondary, fontSize = 8.sp, letterSpacing = 0.5.sp)
                    androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
                    SimpleLineChart(
                        values = balance.map { it.second.toFloat() }.ifEmpty { listOf(current.toFloat()) },
                        chartHeight = 60.dp,
                        lineColor = InfoBlue
                    )
                }
            }

            // KPI strip
            item {
                KpiStrip(
                    items = listOf(
                        Triple(current.toString(),   "Current",  InfoBlue),
                        Triple("+$totalIn",          "In",       Success),
                        Triple("-$totalOut",         "Out",      ErrorRed)
                    )
                )
            }

            // Ledger rows — flat with dividers
            if (ledger.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Outlined.HistoryToggleOff, contentDescription = null,
                                tint = TextSecondary, modifier = Modifier.size(36.dp)
                            )
                            Text("No stock movements yet", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                items(ledger, key = { it.id }) { mov ->
                    val isLast = mov == ledger.last()
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = mov.description,
                                    color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.W500
                                )
                                Text(
                                    text = "${DateUtils.formatTime(mov.timestamp)} • by ${mov.doneByName}",
                                    color = TextSecondary, fontSize = 9.sp
                                )
                            }
                            when (mov.type) {
                                StockChangeType.PRODUCTION -> StatusBadge("Production", BadgeType.SUCCESS)
                                StockChangeType.SALE       -> StatusBadge("Sale",       BadgeType.INFO)
                                StockChangeType.LOSS       -> StatusBadge("Loss",       BadgeType.ERROR)
                                StockChangeType.ADJUSTMENT -> StatusBadge("Adjust",     BadgeType.WARNING)
                            }
                            Text(
                                text = if (mov.quantityChange >= 0) "+${mov.quantityChange}"
                                       else mov.quantityChange.toString(),
                                color = if (mov.quantityChange >= 0) Success else ErrorRed,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.W600
                            )
                        }
                        if (!isLast) {
                            Box(Modifier.fillMaxWidth().height(0.5.dp).background(OverlayWhite07))
                        }
                    }
                }
            }
        }
    }
}
