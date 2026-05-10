package com.kulhad.manager.ui.screens.dashboard

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddBusiness
import androidx.compose.material.icons.outlined.AddCard
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.PrecisionManufacturing
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kulhad.manager.data.util.Money
import com.kulhad.manager.ui.charts.DonutChart
import com.kulhad.manager.ui.charts.SimpleBarChart
import com.kulhad.manager.ui.components.HeroCard
import com.kulhad.manager.ui.components.KpiStrip
import com.kulhad.manager.ui.components.LoadingState
import com.kulhad.manager.ui.components.SectionHeader
import com.kulhad.manager.ui.components.WorkerAvatar
import com.kulhad.manager.ui.preview.UiDemoData
import com.kulhad.manager.ui.theme.BgDeep
import com.kulhad.manager.ui.theme.ErrorRed
import com.kulhad.manager.ui.theme.InfoBlue
import com.kulhad.manager.ui.theme.PrimaryBlue
import com.kulhad.manager.ui.theme.PurpleAccent
import com.kulhad.manager.ui.theme.Success
import com.kulhad.manager.ui.theme.SurfaceCard
import com.kulhad.manager.ui.theme.TextPrimary
import com.kulhad.manager.ui.theme.TextSecondary
import com.kulhad.manager.ui.theme.WarningAmber

@Composable
fun DashboardScreen(
    onAttendance: () -> Unit,
    onAddProduction: () -> Unit,
    onCreateSale: () -> Unit,
    onAddExpense: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenStock: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize().background(BgDeep)) {
        when (state) {
            is DashboardUiState.Loading -> LoadingState()
            is DashboardUiState.Error -> Text(
                text = (state as DashboardUiState.Error).message,
                color = ErrorRed,
                modifier = Modifier.padding(16.dp)
            )
            is DashboardUiState.Success -> {
                val raw = (state as DashboardUiState.Success).data
                // ── Demo overlay: use demo data when DB is empty ─────────────
                val isEmpty = UiDemoData.SHOW_DEMO &&
                        raw.totalRevenueMonth == 0 &&
                        raw.production7Days.all { it == 0 }
                val net      = if (isEmpty) UiDemoData.dashNetProfit     else raw.netProfitMonth.toLong()
                val revenue  = if (isEmpty) UiDemoData.dashRevenue       else raw.totalRevenueMonth.toLong()
                val costs    = if (isEmpty) UiDemoData.dashCosts         else raw.totalCostMonth.toLong()
                val prod7d   = if (isEmpty) UiDemoData.dashProduction7d  else raw.production7Days
                val labels7d = if (isEmpty) UiDemoData.dashLabels7d      else raw.production7DayLabels
                val pieces   = if (isEmpty) UiDemoData.dashPiecesToday   else raw.piecesToday
                val sales    = if (isEmpty) UiDemoData.dashSalesToday    else raw.salesToday.toLong()
                val present  = if (isEmpty) UiDemoData.dashWorkersPresent else raw.workersPresent
                val total    = if (isEmpty) UiDemoData.dashWorkersTotal   else raw.workersTotal
                val alerts   = if (isEmpty) UiDemoData.dashStockAlerts    else raw.stockAlertCount

                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Greeting row
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                Text(text = raw.greeting, color = TextSecondary, fontSize = 10.sp)
                                Text(text = raw.userName, color = TextPrimary,
                                    fontSize = 16.sp, fontWeight = FontWeight.W500)
                            }
                            WorkerAvatar(name = raw.userName, size = 34.dp, fontSize = 11)
                        }
                    }

                    // Hero card: Net profit + donut
                    item {
                        HeroCard(
                            label = "Net profit this month",
                            value = Money.formatRupees(net),
                            valueColor = if (net >= 0) Success else ErrorRed,
                            change = if (UiDemoData.SHOW_DEMO && isEmpty)
                                "↑ 12% from last month" else null,
                            changeColor = Success,
                            trailingContent = {
                                DonutChart(
                                    primaryValue = revenue.toFloat().coerceAtLeast(0f),
                                    secondaryValue = costs.toFloat().coerceAtLeast(0f),
                                    primaryColor = Success,
                                    secondaryColor = ErrorRed,
                                    size = 60.dp,
                                    strokeWidth = 8.dp
                                )
                            },
                            extraContent = {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("Revenue ${Money.formatRupees(revenue)}",
                                        color = TextSecondary, fontSize = 9.sp)
                                    Text("Costs ${Money.formatRupees(costs)}",
                                        color = TextSecondary, fontSize = 9.sp)
                                }
                            }
                        )
                    }

                    // KPI strip: pieces · sales · present/total · alerts
                    item {
                        KpiStrip(
                            items = listOf(
                                Triple("${present}/${total}", "Workers",   InfoBlue),
                                Triple(pieces.toString(),     "Pieces",    WarningAmber),
                                Triple(Money.formatRupees(sales), "Sales", Success),
                                Triple(alerts.toString(),     "Alerts",
                                    if (alerts > 0) ErrorRed else TextPrimary)
                            )
                        )
                    }

                    // 7-day production chart
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
                                values = prod7d.map { it.toFloat() },
                                labels = labels7d,
                                barColor = PrimaryBlue,
                                chartHeight = 72.dp
                            )
                        }
                    }

                    // Quick-action tiles
                    item { SectionHeader(text = "Quick actions") }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            QuickAction("Attendance",  Icons.Outlined.Checklist,             Success,      Modifier.weight(1f), onAttendance)
                            QuickAction("Production",  Icons.Outlined.PrecisionManufacturing, PrimaryBlue, Modifier.weight(1f), onAddProduction)
                            QuickAction("New Sale",    Icons.Outlined.AddCard,                PurpleAccent, Modifier.weight(1f), onCreateSale)
                            QuickAction("Expense",     Icons.Outlined.AddBusiness,            WarningAmber, Modifier.weight(1f), onAddExpense)
                        }
                    }

                    // Shortcut tiles
                    item { SectionHeader(text = "Shortcuts") }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ShortcutTile("Reports", Icons.Outlined.Checklist,  Modifier.weight(1f), onOpenReports)
                            ShortcutTile("Stock",   Icons.Outlined.Inventory2, Modifier.weight(1f), onOpenStock)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAction(
    label: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceCard)
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null,
                tint = accent, modifier = Modifier.size(16.dp))
        }
        Text(text = label, color = TextSecondary, fontSize = 9.sp)
    }
}

@Composable
private fun ShortcutTile(
    label: String,
    icon: ImageVector,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceCard)
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null,
            tint = TextPrimary, modifier = Modifier.size(18.dp))
        Text(text = label, color = TextPrimary, fontSize = 12.sp)
    }
}
