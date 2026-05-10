package com.kulhad.manager.ui.screens.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import com.kulhad.manager.data.local.entity.WorkerType
import com.kulhad.manager.data.util.DateUtils
import com.kulhad.manager.data.util.Money
import com.kulhad.manager.ui.charts.HorizontalBarChart
import com.kulhad.manager.ui.components.BadgeType
import com.kulhad.manager.ui.components.KulhadTopBar
import com.kulhad.manager.ui.components.SectionHeader
import com.kulhad.manager.ui.components.StatCard
import com.kulhad.manager.ui.components.StatusBadge
import com.kulhad.manager.ui.components.WorkerAvatar
import com.kulhad.manager.ui.theme.BgDeep
import com.kulhad.manager.ui.theme.ErrorRed
import com.kulhad.manager.ui.theme.Success
import com.kulhad.manager.ui.theme.SurfaceCard
import com.kulhad.manager.ui.theme.TextPrimary
import com.kulhad.manager.ui.theme.TextSecondary
import com.kulhad.manager.ui.theme.WarningAmber

@Composable
fun SalaryReportScreen(
    onBack: () -> Unit,
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val month by viewModel.monthAnchor.collectAsStateWithLifecycle()
    val report by viewModel.salary.collectAsStateWithLifecycle()

    LaunchedEffect(month) { viewModel.loadSalary() }

    Column(modifier = Modifier.fillMaxSize().background(BgDeep)) {
        KulhadTopBar(
            title = "Salary Report",
            subtitle = DateUtils.formatMonth(month),
            onBack = onBack,
            actions = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Prev",
                    tint = TextPrimary,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { viewModel.prevMonth() }
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next",
                    tint = TextPrimary,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { viewModel.nextMonth() }
                )
            }
        )
        LazyColumn(
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard(
                        value = Money.formatRupees(report?.totalPayout ?: 0),
                        label = "Total payout",
                        valueColor = Success,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        value = Money.formatRupees(report?.totalAdvances ?: 0),
                        label = "Advances",
                        valueColor = ErrorRed,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            item { SectionHeader(text = "Top earners") }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceCard)
                        .padding(12.dp)
                ) {
                    val rows = report?.rows.orEmpty()
                        .filter { it.grossEarnings > 0 }
                        .take(5)
                        .map { it.workerName to it.grossEarnings.toFloat() }
                    if (rows.isEmpty()) {
                        Text("No salary data this month", color = TextSecondary, fontSize = 11.sp)
                    } else {
                        HorizontalBarChart(items = rows, barColor = WarningAmber)
                    }
                }
            }
            item { SectionHeader(text = "Workers") }
            items(report?.rows.orEmpty(), key = { it.workerId }) { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceCard)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    WorkerAvatar(name = row.workerName, size = 32.dp, fontSize = 10)
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(row.workerName, color = TextPrimary, fontSize = 12.sp,
                                fontWeight = FontWeight.W500)
                            StatusBadge(
                                if (row.workerType == WorkerType.PIECE) "Piece" else "Salary",
                                if (row.workerType == WorkerType.PIECE) BadgeType.PURPLE else BadgeType.INFO
                            )
                        }
                        val computeLine = if (row.workerType == WorkerType.PIECE)
                            "${row.pieceQty} × rate = ${Money.formatRupeesDouble(row.pieceEarnings)}"
                        else
                            "${row.daysPresent} days × ${Money.formatRupees(row.dailyRate)} = ${Money.formatRupees(row.salaryEarnings)}"
                        Text(text = computeLine, color = TextSecondary, fontSize = 10.sp)
                        if (row.advances > 0) {
                            Text(
                                text = "Adv −${Money.formatRupees(row.advances)} → Net ${Money.formatRupees(row.netEarnings)}",
                                color = ErrorRed,
                                fontSize = 10.sp
                            )
                        }
                    }
                    Text(
                        text = Money.formatRupees(row.netEarnings.coerceAtLeast(0)),
                        color = Success,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.W600
                    )
                }
            }
        }
    }
}
