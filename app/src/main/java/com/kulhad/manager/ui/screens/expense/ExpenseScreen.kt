package com.kulhad.manager.ui.screens.expense

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kulhad.manager.data.util.DateUtils
import com.kulhad.manager.data.util.Money
import com.kulhad.manager.ui.charts.MultiSegmentDonut
import com.kulhad.manager.ui.components.BadgeType
import com.kulhad.manager.ui.components.EmptyState
import com.kulhad.manager.ui.components.KulhadTopBar
import com.kulhad.manager.ui.components.SectionHeader
import com.kulhad.manager.ui.components.StatCard
import com.kulhad.manager.ui.components.StatusBadge
import com.kulhad.manager.ui.theme.BgDeep
import com.kulhad.manager.ui.theme.PrimaryBlue
import com.kulhad.manager.ui.theme.PurpleAccent
import com.kulhad.manager.ui.theme.Success
import com.kulhad.manager.ui.theme.SurfaceCard
import com.kulhad.manager.ui.theme.TextPrimary
import com.kulhad.manager.ui.theme.TextSecondary
import com.kulhad.manager.ui.theme.WarningAmber

@Composable
fun ExpenseScreen(
    onAddExpense: () -> Unit,
    onBack: () -> Unit,
    viewModel: ExpenseViewModel = hiltViewModel()
) {
    val data by viewModel.tabData.collectAsStateWithLifecycle()

    val palette = listOf(WarningAmber, PurpleAccent, PrimaryBlue, Success, Color(0xFFEC4899), Color(0xFF06B6D4))

    Column(modifier = Modifier.fillMaxSize().background(BgDeep)) {
        KulhadTopBar(
            title = "Expenses",
            onBack = onBack,
            actions = {
                IconButton(onClick = onAddExpense) {
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
                        value = Money.formatRupees(data.totalThisMonth),
                        label = "This month",
                        valueColor = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        value = Money.formatRupees(data.laborTotal),
                        label = "Labor",
                        valueColor = WarningAmber,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard(
                        value = Money.formatRupees(data.soilTotal),
                        label = "Soil",
                        valueColor = PurpleAccent,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        value = Money.formatRupees(data.transportTotal),
                        label = "Transport",
                        valueColor = PrimaryBlue,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            item { SectionHeader(text = "Breakdown") }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceCard)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (data.breakdown.isEmpty()) {
                            Text("No expenses this month", color = TextSecondary, fontSize = 11.sp)
                        }
                        data.breakdown.take(5).forEachIndexed { idx, (name, amt) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = name, color = palette[idx % palette.size], fontSize = 11.sp)
                                Text(text = Money.formatRupees(amt), color = TextPrimary, fontSize = 11.sp)
                            }
                        }
                    }
                    MultiSegmentDonut(
                        segments = data.breakdown.mapIndexed { idx, (_, v) ->
                            palette[idx % palette.size] to v.toFloat()
                        },
                        size = 64.dp,
                        strokeWidth = 10.dp
                    )
                }
            }
            item { SectionHeader(text = "Recent expenses") }
            if (data.recent.isEmpty()) {
                item { EmptyState(message = "No expenses recorded yet", icon = Icons.Outlined.Receipt) }
            } else {
                items(data.recent, key = { it.id }) { e ->
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
                            Text(text = Money.formatRupees(e.amount), color = TextPrimary,
                                fontSize = 13.sp, fontWeight = FontWeight.W500)
                            if (e.remark.isNotBlank()) {
                                Text(text = e.remark, color = TextSecondary, fontSize = 10.sp)
                            }
                            Text(text = DateUtils.formatDayShort(e.date), color = TextSecondary, fontSize = 10.sp)
                        }
                        StatusBadge(e.typeName, BadgeType.PURPLE)
                    }
                }
            }
        }
    }
}
