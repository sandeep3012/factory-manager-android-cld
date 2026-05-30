package com.kulhad.manager.ui.screens.production

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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
import com.kulhad.manager.data.util.Money
import com.kulhad.manager.ui.charts.SimpleBarChart
import com.kulhad.manager.ui.components.KpiStrip
import com.kulhad.manager.ui.components.KulhadTopBar
import com.kulhad.manager.ui.components.SectionHeader
import com.kulhad.manager.ui.components.WorkingDateChip
import com.kulhad.manager.ui.preview.UiDemoData
import com.kulhad.manager.ui.theme.BgDeep
import com.kulhad.manager.ui.theme.OverlayWhite07
import com.kulhad.manager.ui.theme.PrimaryBlue
import com.kulhad.manager.ui.theme.PurpleAccent
import com.kulhad.manager.ui.theme.Success
import com.kulhad.manager.ui.theme.SurfaceCard
import com.kulhad.manager.ui.theme.TextPrimary
import com.kulhad.manager.ui.theme.TextSecondary
import com.kulhad.manager.ui.theme.WarningAmber

@Composable
fun ProductionScreen(
    onAddProduction: () -> Unit,
    onHistory: () -> Unit,
    viewModel: ProductionViewModel = hiltViewModel()
) {
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val productsWithRates by viewModel.productsWithRates.collectAsStateWithLifecycle()
    val workingDate by viewModel.workingDate.collectAsStateWithLifecycle()

    val useDemo = UiDemoData.SHOW_DEMO && stats.totalPieces == 0 && stats.daily.all { it == 0 }

    val dispTotal   = if (useDemo) UiDemoData.productionTotal7d  else stats.totalPieces
    val dispDefect  = if (useDemo) UiDemoData.productionDefective else stats.defective
    val dispQuality = if (useDemo) UiDemoData.productionQuality   else stats.qualityPercent
    val dispDaily   = if (useDemo) UiDemoData.productionDaily.map { it.toFloat() } else stats.daily.map { it.toFloat() }
    val dispLabels  = if (useDemo) UiDemoData.productionLabels else stats.labels

    Column(modifier = Modifier.fillMaxSize().background(BgDeep)) {
        KulhadTopBar(
            title = "Production",
            actions = {
                IconButton(onClick = onHistory) {
                    Icon(Icons.Outlined.History, contentDescription = "History", tint = TextPrimary)
                }
                IconButton(onClick = onAddProduction) {
                    Icon(Icons.Filled.Add, contentDescription = "Add", tint = PrimaryBlue)
                }
            }
        )
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Working date chip — tap to change the global working date
            item {
                WorkingDateChip(
                    selectedDate = workingDate,
                    onDateSelected = { viewModel.setWorkingDate(it) }
                )
            }

            // KPI strip — replaces StatCard row
            item {
                KpiStrip(
                    items = listOf(
                        Triple(dispTotal.toString(),    "Pieces (7d)", TextPrimary),
                        Triple(dispDefect.toString(),   "Defective",   WarningAmber),
                        Triple("$dispQuality%",         "Quality",     Success)
                    )
                )
            }

            // 7-day bar chart
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceCard)
                        .padding(14.dp)
                ) {
                    Text(
                        "DAILY PRODUCTION — LAST 7 DAYS",
                        color = TextSecondary, fontSize = 10.sp, letterSpacing = 0.6.sp
                    )
                    androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
                    SimpleBarChart(
                        values = dispDaily,
                        labels = dispLabels,
                        barColor = PurpleAccent,
                        chartHeight = 86.dp
                    )
                }
            }

            // Piece rates — flat rows with dividers
            item { SectionHeader(text = "Piece rates") }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceCard)
                        .padding(horizontal = 14.dp)
                ) {
                    if (useDemo) {
                        UiDemoData.pieceRates.forEachIndexed { idx, (sizeMl, rate) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 13.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${sizeMl}ml",
                                    color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.W500
                                )
                                Text(
                                    text = "₹${"%.2f".format(rate)} / piece",
                                    color = WarningAmber, fontSize = 13.sp
                                )
                            }
                            if (idx < UiDemoData.pieceRates.lastIndex) {
                                Box(Modifier.fillMaxWidth().height(0.5.dp).background(OverlayWhite07))
                            }
                        }
                    } else if (productsWithRates.isEmpty()) {
                        Text(
                            "No products configured",
                            color = TextSecondary, fontSize = 13.sp,
                            modifier = Modifier.padding(vertical = 14.dp)
                        )
                    } else {
                        productsWithRates.forEachIndexed { idx, p ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 13.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = p.product.displayLabel,
                                    color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.W500
                                )
                                Text(
                                    text = "${Money.formatRupeesDouble(p.ratePerPiece)} / piece",
                                    color = WarningAmber, fontSize = 13.sp
                                )
                            }
                            if (idx < productsWithRates.lastIndex) {
                                Box(Modifier.fillMaxWidth().height(0.5.dp).background(OverlayWhite07))
                            }
                        }
                    }
                }
            }
        }
    }
}
