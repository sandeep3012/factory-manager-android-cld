package com.kulhad.manager.ui.screens.reports

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
import com.kulhad.manager.ui.components.HeroCardDual
import com.kulhad.manager.ui.components.KulhadTopBar
import com.kulhad.manager.ui.components.SectionHeader
import com.kulhad.manager.ui.components.StatusBadge
import com.kulhad.manager.ui.components.WorkerAvatar
import com.kulhad.manager.ui.preview.UiDemoData
import com.kulhad.manager.ui.theme.BgDeep
import com.kulhad.manager.ui.theme.ErrorRed
import com.kulhad.manager.ui.theme.OverlayWhite07
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

    val useDemo = UiDemoData.SHOW_DEMO && report == null

    val dispPayout   = if (useDemo) UiDemoData.salaryTotalPayout   else (report?.totalPayout?.toLong() ?: 0L)
    val dispAdvances = if (useDemo) UiDemoData.salaryTotalAdvances  else (report?.totalAdvances?.toLong() ?: 0L)

    val demoTopEarners: List<Pair<String, Float>> = UiDemoData.salaryRows.map { it.name to (it.amount.replace("₹","").replace(",","").toFloatOrNull() ?: 0f) }
    val realTopEarners: List<Pair<String, Float>> = report?.rows.orEmpty().filter { it.grossEarnings > 0 }.take(5).map { it.workerName to it.grossEarnings.toFloat() }
    val topEarners: List<Pair<String, Float>> = if (useDemo) demoTopEarners else realTopEarners

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
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Dual hero card: payout + advances
            item {
                HeroCardDual(
                    primaryLabel = "Total Payout",
                    primaryValue = Money.formatRupees(dispPayout),
                    primaryColor = Success,
                    secondaryLabel = "Advances",
                    secondaryValue = Money.formatRupees(dispAdvances),
                    secondaryColor = ErrorRed
                )
            }

            // Top earners horizontal bar chart
            item { SectionHeader(text = "Top earners") }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceCard)
                        .padding(12.dp)
                ) {
                    if (topEarners.isEmpty()) {
                        Text("No salary data this month", color = TextSecondary, fontSize = 11.sp)
                    } else {
                        HorizontalBarChart(items = topEarners, barColor = WarningAmber)
                    }
                }
            }

            // Workers list — flat rows with dividers
            item { SectionHeader(text = "Workers") }

            if (useDemo) {
                items(UiDemoData.salaryRows) { row ->
                    val isLast = row == UiDemoData.salaryRows.last()
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            WorkerAvatar(name = row.name, size = 38.dp, fontSize = 12)
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(row.name, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.W500)
                                    StatusBadge(
                                        row.type,
                                        if (row.type == "Piece") BadgeType.PURPLE else BadgeType.INFO
                                    )
                                }
                                Text(text = row.computation, color = TextSecondary, fontSize = 14.sp)
                            }
                            Text(
                                text = row.amount,
                                color = Success, fontSize = 16.sp, fontWeight = FontWeight.W600
                            )
                        }
                        if (!isLast) {
                            Box(Modifier.fillMaxWidth().height(0.5.dp).background(OverlayWhite07))
                        }
                    }
                }
            } else {
                items(report?.rows.orEmpty(), key = { it.workerId }) { row ->
                    val isLast = row == report?.rows?.last()
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            WorkerAvatar(name = row.workerName, size = 38.dp, fontSize = 12)
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(row.workerName, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.W500)
                                    StatusBadge(
                                        if (row.workerType == WorkerType.PIECE) "Piece" else "Salary",
                                        if (row.workerType == WorkerType.PIECE) BadgeType.PURPLE else BadgeType.INFO
                                    )
                                }
                                val computeLine = if (row.workerType == WorkerType.PIECE)
                                    "${row.pieceQty} × rate = ${Money.formatRupeesDouble(row.pieceEarnings)}"
                                else
                                    "${row.daysPresent} days × ${Money.formatRupees(row.dailyRate.toLong())} = ${Money.formatRupees(row.salaryEarnings.toLong())}"
                                Text(text = computeLine, color = TextSecondary, fontSize = 14.sp)
                                if (row.advances > 0) {
                                    Text(
                                        text = "Adv −${Money.formatRupees(row.advances.toLong())} → Net ${Money.formatRupees(row.netEarnings.toLong())}",
                                        color = ErrorRed, fontSize = 14.sp
                                    )
                                }
                            }
                            Text(
                                text = Money.formatRupees(row.netEarnings.coerceAtLeast(0).toLong()),
                                color = Success, fontSize = 16.sp, fontWeight = FontWeight.W600
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
