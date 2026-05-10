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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddBusiness
import androidx.compose.material.icons.outlined.AddCard
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Inventory2
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
import com.kulhad.manager.ui.components.LoadingState
import com.kulhad.manager.ui.components.SectionHeader
import com.kulhad.manager.ui.components.StatCard
import com.kulhad.manager.ui.components.WorkerAvatar
import com.kulhad.manager.ui.theme.BgDeep
import com.kulhad.manager.ui.theme.ErrorRed
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
    ) {
        when (state) {
            is DashboardUiState.Loading -> LoadingState()
            is DashboardUiState.Error -> Text(
                text = (state as DashboardUiState.Error).message,
                color = ErrorRed,
                modifier = Modifier.padding(16.dp)
            )
            is DashboardUiState.Success -> {
                val data = (state as DashboardUiState.Success).data
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item { GreetingHeader(data) }
                    item { ProfitHeroCard(data) }
                    item { SectionHeader(text = "7-day production") }
                    item { ProductionChartCard(data) }
                    item { KpiGrid(data) }
                    item { SectionHeader(text = "Quick actions") }
                    item {
                        QuickActions(
                            onAttendance, onAddProduction, onCreateSale, onAddExpense
                        )
                    }
                    item { SectionHeader(text = "Shortcuts") }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ShortcutTile(
                                "Reports", Icons.Outlined.Checklist,
                                Modifier.weight(1f), onOpenReports
                            )
                            ShortcutTile(
                                "Stock", Icons.Outlined.Inventory2,
                                Modifier.weight(1f), onOpenStock
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GreetingHeader(data: DashboardData) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = data.greeting, color = TextSecondary, fontSize = 11.sp)
            Text(
                text = data.userName,
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.W500
            )
        }
        WorkerAvatar(name = data.userName, size = 36.dp, fontSize = 11)
    }
}

@Composable
private fun ProfitHeroCard(data: DashboardData) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceCard)
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "NET PROFIT • THIS MONTH",
                color = TextSecondary,
                fontSize = 9.sp,
                letterSpacing = 0.5.sp
            )
            Text(
                text = Money.formatRupees(data.netProfitMonth),
                color = if (data.netProfitMonth >= 0) Success else ErrorRed,
                fontSize = 24.sp,
                fontWeight = FontWeight.W600
            )
            Text(
                text = "Revenue ${Money.formatRupees(data.totalRevenueMonth)}",
                color = TextSecondary,
                fontSize = 10.sp
            )
            Text(
                text = "Costs ${Money.formatRupees(data.totalCostMonth)}",
                color = TextSecondary,
                fontSize = 10.sp
            )
        }
        DonutChart(
            primaryValue = data.netProfitMonth.coerceAtLeast(0).toFloat(),
            secondaryValue = data.totalCostMonth.toFloat(),
            size = 64.dp,
            strokeWidth = 10.dp
        )
    }
}

@Composable
private fun ProductionChartCard(data: DashboardData) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceCard)
            .padding(12.dp)
    ) {
        SimpleBarChart(
            values = data.production7Days.map { it.toFloat() },
            labels = data.production7DayLabels,
            barColor = PrimaryBlue,
            chartHeight = 72.dp
        )
    }
}

@Composable
private fun KpiGrid(data: DashboardData) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(
                value = data.piecesToday.toString(),
                label = "Pieces today",
                modifier = Modifier.weight(1f),
                valueColor = TextPrimary
            )
            StatCard(
                value = Money.formatRupees(data.salesToday),
                label = "Sales today",
                modifier = Modifier.weight(1f),
                valueColor = Success
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(
                value = "${data.workersPresent}/${data.workersTotal}",
                label = "Workers present",
                modifier = Modifier.weight(1f),
                valueColor = PrimaryBlue
            )
            StatCard(
                value = data.stockAlertCount.toString(),
                label = "Stock alerts",
                modifier = Modifier.weight(1f),
                valueColor = if (data.stockAlertCount > 0) WarningAmber else TextPrimary
            )
        }
    }
}

@Composable
private fun QuickActions(
    onAttendance: () -> Unit,
    onAddProduction: () -> Unit,
    onCreateSale: () -> Unit,
    onAddExpense: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuickAction("Attendance", Icons.Outlined.Checklist, Success, Modifier.weight(1f), onAttendance)
        QuickAction("Add Prod.", Icons.Outlined.AddBusiness, PrimaryBlue, Modifier.weight(1f), onAddProduction)
        QuickAction("New Sale", Icons.Outlined.AddCard, PurpleAccent, Modifier.weight(1f), onCreateSale)
        QuickAction("Expense", Icons.Outlined.AddCard, WarningAmber, Modifier.weight(1f), onAddExpense)
    }
}

@Composable
private fun QuickAction(
    label: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceCard)
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
        }
        Text(text = label, color = TextSecondary, fontSize = 10.sp)
    }
}

@Composable
private fun ShortcutTile(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceCard)
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(20.dp))
        Text(text = label, color = TextPrimary, fontSize = 12.sp)
    }
}
