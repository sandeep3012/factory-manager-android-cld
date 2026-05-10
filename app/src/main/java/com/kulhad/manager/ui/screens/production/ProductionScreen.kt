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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Inventory
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
import com.kulhad.manager.ui.components.EmptyState
import com.kulhad.manager.ui.components.KulhadTopBar
import com.kulhad.manager.ui.components.SectionHeader
import com.kulhad.manager.ui.components.StatCard
import com.kulhad.manager.ui.theme.BgDeep
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
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard(
                        value = stats.totalPieces.toString(),
                        label = "Pieces (7d)",
                        valueColor = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        value = stats.defective.toString(),
                        label = "Defective",
                        valueColor = WarningAmber,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        value = "${stats.qualityPercent}%",
                        label = "Quality",
                        valueColor = Success,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            item { SectionHeader(text = "7-day production") }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceCard)
                        .padding(12.dp)
                ) {
                    SimpleBarChart(
                        values = stats.daily.map { it.toFloat() },
                        labels = stats.labels,
                        barColor = PurpleAccent,
                        chartHeight = 78.dp
                    )
                }
            }
            item { SectionHeader(text = "Piece rates") }
            if (productsWithRates.isEmpty()) {
                item { EmptyState(message = "No products configured", icon = Icons.Outlined.Inventory) }
            } else {
                items(productsWithRates, key = { it.product.id }) { p ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceCard)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${p.product.sizeMl}ml",
                            color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.W500,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = Money.formatRupeesDouble(p.ratePerPiece) + " / piece",
                            color = WarningAmber, fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}
