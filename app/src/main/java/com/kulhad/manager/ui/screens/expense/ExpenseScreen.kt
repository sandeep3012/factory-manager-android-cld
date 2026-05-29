package com.kulhad.manager.ui.screens.expense

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.History
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
import com.kulhad.manager.ui.components.HeroCard
import com.kulhad.manager.ui.components.KpiStrip
import com.kulhad.manager.ui.components.KulhadTopBar
import com.kulhad.manager.ui.components.ReportRow
import com.kulhad.manager.ui.components.SectionHeader
import com.kulhad.manager.ui.components.StatusBadge
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
fun ExpenseScreen(
    onAddExpense: () -> Unit,
    onHistory: () -> Unit,
    onBack: () -> Unit,
    viewModel: ExpenseViewModel = hiltViewModel()
) {
    val data by viewModel.tabData.collectAsStateWithLifecycle()

    val useDemo = UiDemoData.SHOW_DEMO && data.totalThisMonth == 0
    val dispTotal     = if (useDemo) UiDemoData.expenseTotal     else data.totalThisMonth.toLong()
    val dispLabor     = if (useDemo) UiDemoData.expenseLabor     else data.laborTotal.toLong()
    val dispSoil      = if (useDemo) UiDemoData.expenseSoil      else data.soilTotal.toLong()
    val dispTransport = if (useDemo) UiDemoData.expenseTransport else data.transportTotal.toLong()
    val dispBreakdown: List<Pair<String, Long>> =
        if (useDemo) UiDemoData.expenseBreakdown
        else data.breakdown.map { (k, v) -> k to v.toLong() }

    val palette = listOf(WarningAmber, PurpleAccent, PrimaryBlue, Success,
        Color(0xFFEC4899), Color(0xFF06B6D4))

    Column(modifier = Modifier.fillMaxSize().background(BgDeep)) {
        KulhadTopBar(
            title = "Expenses / व्यय",
            onBack = onBack,
            actions = {
                IconButton(onClick = onHistory) {
                    Icon(Icons.Outlined.History, contentDescription = "Expense History",
                        tint = TextPrimary)
                }
                IconButton(onClick = onAddExpense) {
                    Icon(Icons.Filled.Add, contentDescription = "Add", tint = PrimaryBlue)
                }
            }
        )

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Hero card (finance style, matching HTML screen 8)
            item {
                HeroCard(
                    label = "Total expenses this month",
                    value = Money.formatRupees(dispTotal),
                    valueColor = WarningAmber,
                    trailingContent = {
                        MultiSegmentDonut(
                            segments = dispBreakdown.mapIndexed { idx, (_, v) ->
                                palette[idx % palette.size] to v.toFloat()
                            }.ifEmpty { listOf(WarningAmber to 1f) },
                            size = 67.dp,
                            strokeWidth = 10.dp
                        )
                    }
                )
            }

            // KPI strip: labor · soil · transport
            item {
                KpiStrip(
                    items = listOf(
                        Triple(Money.formatRupees(dispLabor),     "Labor",     WarningAmber),
                        Triple(Money.formatRupees(dispSoil),      "Soil",      PurpleAccent),
                        Triple(Money.formatRupees(dispTransport), "Transport", PrimaryBlue)
                    )
                )
            }

            // Finance-style breakdown (HTML screen 8 — report rows)
            item { SectionHeader(text = "Breakdown") }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceCard)
                        .padding(horizontal = 12.dp)
                ) {
                    if (dispBreakdown.isEmpty()) {
                        Text("No expenses this month", color = TextSecondary, fontSize = 13.sp,
                            modifier = Modifier.padding(vertical = 12.dp))
                    } else {
                        dispBreakdown.forEachIndexed { idx, (name, amt) ->
                            ReportRow(
                                label = name,
                                value = Money.formatRupees(amt),
                                valueColor = palette[idx % palette.size],
                                showDivider = idx < dispBreakdown.lastIndex
                            )
                        }
                    }
                }
            }

            // Recent expense list
            item { SectionHeader(text = "Recent expenses") }

            if (useDemo) {
                items(UiDemoData.recentExpenses) { e ->
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = e.amount, color = TextPrimary,
                                    fontSize = 16.sp, fontWeight = FontWeight.W500)
                                Text(text = e.remark, color = TextSecondary, fontSize = 14.sp)
                                Text(text = e.date, color = TextSecondary, fontSize = 13.sp)
                            }
                            StatusBadge(e.type, BadgeType.PURPLE)
                        }
                        Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(OverlayWhite07))
                    }
                }
            } else if (data.recent.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                        contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Outlined.Receipt, contentDescription = null,
                                tint = TextSecondary, modifier = Modifier.size(43.dp))
                            Text("No expenses recorded yet", color = TextSecondary, fontSize = 14.sp)
                        }
                    }
                }
            } else {
                items(data.recent, key = { it.id }) { e ->
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = Money.formatRupees(e.amount), color = TextPrimary,
                                    fontSize = 16.sp, fontWeight = FontWeight.W500)
                                if (e.remark.isNotBlank()) {
                                    Text(text = e.remark, color = TextSecondary, fontSize = 14.sp)
                                }
                                Text(text = DateUtils.formatDayShort(e.date),
                                    color = TextSecondary, fontSize = 13.sp)
                            }
                            StatusBadge(e.typeName, BadgeType.PURPLE)
                        }
                        Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(OverlayWhite07))
                    }
                }
            }
        }
    }
}
