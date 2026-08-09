package com.kulhad.manager.ui.screens.workers

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Construction
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.MoneyOff
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.kulhad.manager.data.local.entity.ProductionEntryEntity
import com.kulhad.manager.data.local.entity.WorkerType
import com.kulhad.manager.data.repository.WorkerRepository
import com.kulhad.manager.data.util.DateUtils
import com.kulhad.manager.data.util.Money
import com.kulhad.manager.domain.model.AttendanceRecord
import com.kulhad.manager.domain.model.Worker
import com.kulhad.manager.domain.model.WorkerAdvanceRecord
import com.kulhad.manager.ui.components.BadgeType
import com.kulhad.manager.ui.components.KulhadTopBar
import com.kulhad.manager.ui.components.StatusBadge
import com.kulhad.manager.ui.components.WorkerAvatar
import com.kulhad.manager.ui.theme.BgDeep
import com.kulhad.manager.ui.theme.ErrorRed
import com.kulhad.manager.ui.theme.InfoBlue
import com.kulhad.manager.ui.theme.OverlayWhite07
import com.kulhad.manager.ui.theme.PrimaryBlue
import com.kulhad.manager.ui.theme.Success
import com.kulhad.manager.ui.theme.SurfaceCard
import com.kulhad.manager.ui.theme.TextPrimary
import com.kulhad.manager.ui.theme.TextSecondary
import com.kulhad.manager.ui.theme.TextTertiary
import com.kulhad.manager.ui.theme.WarningAmber
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first

// ── Domain model ─────────────────────────────────────────────────────────────

data class WorkerHistoryState(
    val worker: Worker? = null,
    // Attendance
    val totalPresent: Int = 0,
    val totalAbsent: Int = 0,
    // Production (PIECE workers only)
    val productionEntryCount: Int = 0,
    val totalNetQty: Int = 0,
    val totalDefective: Int = 0,
    val totalPieceEarnings: Double = 0.0,
    // Advance
    val totalAdvances: Int = 0,
    val lastAdvanceDate: Long? = null,
    // Salary (SALARY workers only — computed from lifetime present × dailyRate)
    val computedSalaryEarned: Int = 0,
    // Recent activity
    val recentAttendance: List<AttendanceRecord> = emptyList(),
    val recentProduction: List<ProductionEntryEntity> = emptyList(),
    val recentAdvances: List<WorkerAdvanceRecord> = emptyList(),
    val isLoading: Boolean = true
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class WorkerHistoryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: WorkerRepository
) : ViewModel() {

    private val workerId: Long = checkNotNull(savedStateHandle["workerId"])

    val state: StateFlow<WorkerHistoryState> = flow {
        emit(WorkerHistoryState(isLoading = true))

        val worker = repository.getWorker(workerId)

        // Aggregate suspend queries
        val totalPresent = repository.countAllPresentForWorker(workerId)
        val totalAbsent = repository.countAllAbsentForWorker(workerId)
        val entryCount = repository.countProductionEntriesForWorker(workerId)
        val netQty = repository.totalNetQtyForWorker(workerId)
        val defective = repository.totalDefectiveForWorker(workerId)
        val pieceEarnings = repository.totalEarningsForWorker(workerId)
        val totalAdvances = repository.totalAdvancesForWorker(workerId)

        // Computed salary: present days × current daily rate (best estimate for lifetime)
        val computedSalary = if (worker?.currentType == WorkerType.SALARY) {
            totalPresent * worker.dailyRate
        } else 0

        // Take the first snapshot from each recent-activity Flow
        val recentAttendance = repository.observeRecentAttendanceForWorker(workerId, 5).first()
        val recentProduction = repository.observeRecentProductionForWorker(workerId, 5).first()
        val recentAdvances = repository.observeRecentAdvancesForWorker(workerId, 5).first()
        val lastAdvanceDate = recentAdvances.firstOrNull()?.date

        emit(
            WorkerHistoryState(
                worker = worker,
                totalPresent = totalPresent,
                totalAbsent = totalAbsent,
                productionEntryCount = entryCount,
                totalNetQty = netQty,
                totalDefective = defective,
                totalPieceEarnings = pieceEarnings,
                totalAdvances = totalAdvances,
                lastAdvanceDate = lastAdvanceDate,
                computedSalaryEarned = computedSalary,
                recentAttendance = recentAttendance,
                recentProduction = recentProduction,
                recentAdvances = recentAdvances,
                isLoading = false
            )
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        WorkerHistoryState(isLoading = true)
    )
}

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun WorkerHistoryScreen(
    workerId: Long,
    onBack: () -> Unit,
    onViewProductionDetail: (Long) -> Unit = {},
    onViewAttendanceDetail: (Long, Long) -> Unit = { _, _ -> },
    onViewAdvanceHistory: (Long) -> Unit = {},
    viewModel: WorkerHistoryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val worker = state.worker

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
    ) {
        KulhadTopBar(
            title = worker?.name ?: "Worker History",
            subtitle = if (worker != null) {
                "${worker.currentType.name.lowercase().replaceFirstChar { it.uppercase() }} worker"
            } else "Loading…",
            onBack = onBack
        )

        if (state.isLoading || worker == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading…", color = TextSecondary, fontSize = 14.sp)
            }
            return@Column
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Header card ───────────────────────────────────────────────
            item { WorkerHeaderCard(state) }

            // ── Attendance summary ────────────────────────────────────────
            item {
                AttendanceSummaryCard(
                    state = state,
                    onViewDetail = {
                        onViewAttendanceDetail(workerId, DateUtils.startOfMonth(System.currentTimeMillis()))
                    }
                )
            }

            // ── Production summary (PIECE workers) ────────────────────────
            if (worker.currentType == WorkerType.PIECE) {
                item {
                    ProductionSummaryCard(
                        state = state,
                        onViewDetail = { onViewProductionDetail(workerId) }
                    )
                }
            }

            // ── Salary summary (SALARY workers) ───────────────────────────
            if (worker.currentType == WorkerType.SALARY) {
                item { SalarySummaryCard(state) }
            }

            // ── Advance summary ───────────────────────────────────────────
            item {
                AdvanceSummaryCard(
                    state = state,
                    onViewDetail = { onViewAdvanceHistory(workerId) }
                )
            }

            // ── Recent activity ───────────────────────────────────────────
            item { RecentActivityCard(state) }
        }
    }
}

// ── Cards ─────────────────────────────────────────────────────────────────────

@Composable
private fun WorkerHeaderCard(state: WorkerHistoryState) {
    val worker = state.worker ?: return
    val isPiece = worker.currentType == WorkerType.PIECE

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceCard)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            WorkerAvatar(name = worker.name, size = 52.dp, fontSize = 18)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = worker.name,
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (!worker.isActive) {
                        StatusBadge("Inactive", BadgeType.WARNING)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusBadge(
                        worker.currentType.name.lowercase().replaceFirstChar { it.uppercase() },
                        if (isPiece) BadgeType.INFO else BadgeType.SUCCESS
                    )
                    if (!isPiece) {
                        StatusBadge(Money.formatRupees(worker.dailyRate) + "/day", BadgeType.INFO)
                    }
                }
                Text(
                    text = "Joined ${DateUtils.formatDay(worker.joiningDate)}",
                    color = TextTertiary,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun AttendanceSummaryCard(state: WorkerHistoryState, onViewDetail: () -> Unit = {}) {
    val total = state.totalPresent + state.totalAbsent
    val pct = if (total > 0) (state.totalPresent * 100) / total else 0

    HistoryCard(title = "Attendance Summary", icon = Icons.Outlined.DateRange) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            KpiItem(label = "Present", value = state.totalPresent.toString(), color = Success)
            KpiItem(label = "Absent", value = state.totalAbsent.toString(), color = ErrorRed)
            KpiItem(label = "Total Days", value = total.toString(), color = TextPrimary)
            KpiItem(label = "Rate", value = "$pct%", color = InfoBlue)
        }
        Text(
            text     = "View month-wise attendance →",
            color    = PrimaryBlue,
            fontSize = 13.sp,
            fontWeight = FontWeight.W500,
            modifier = Modifier
                .clickable { onViewDetail() }
                .padding(top = 4.dp)
        )
    }
}

@Composable
private fun ProductionSummaryCard(state: WorkerHistoryState, onViewDetail: () -> Unit = {}) {
    HistoryCard(title = "Production Summary", icon = Icons.Outlined.Construction) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            KpiItem(label = "Sessions", value = state.productionEntryCount.toString(), color = TextPrimary)
            KpiItem(label = "Good Qty", value = state.totalNetQty.toString(), color = Success)
            KpiItem(label = "Defective", value = state.totalDefective.toString(), color = ErrorRed)
            KpiItem(
                label = "Earned",
                value = Money.formatRupees(state.totalPieceEarnings.toInt()),
                color = PrimaryBlue
            )
        }
        Text(
            text     = "View month-wise production →",
            color    = PrimaryBlue,
            fontSize = 13.sp,
            fontWeight = FontWeight.W500,
            modifier = Modifier
                .clickable { onViewDetail() }
                .padding(top = 4.dp)
        )
    }
}

@Composable
private fun SalarySummaryCard(state: WorkerHistoryState) {
    val worker = state.worker ?: return
    HistoryCard(title = "Salary Summary", icon = Icons.Outlined.Payments) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryRow(
                label = "Days Present (all time)",
                value = state.totalPresent.toString()
            )
            SummaryRow(
                label = "Current Daily Rate",
                value = Money.formatRupees(worker.dailyRate)
            )
            HorizontalDivider()
            SummaryRow(
                label = "Est. Total Salary Earned",
                value = Money.formatRupees(state.computedSalaryEarned),
                highlight = true
            )
            Text(
                text = "* Computed as present days × current rate. Historical rate changes not applied.",
                color = TextTertiary,
                fontSize = 11.sp,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
private fun AdvanceSummaryCard(state: WorkerHistoryState, onViewDetail: () -> Unit = {}) {
    HistoryCard(title = "Advance Summary", icon = Icons.Outlined.AttachMoney) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryRow(
                label = "Total Advances Given",
                value = Money.formatRupees(state.totalAdvances),
                highlight = true
            )
            if (state.lastAdvanceDate != null) {
                SummaryRow(
                    label = "Last Advance",
                    value = DateUtils.formatDay(state.lastAdvanceDate)
                )
            } else {
                Text("No advances recorded", color = TextTertiary, fontSize = 13.sp)
            }
        }
        Text(
            text     = "View month-wise advances →",
            color    = PrimaryBlue,
            fontSize = 13.sp,
            fontWeight = FontWeight.W500,
            modifier = Modifier
                .clickable { onViewDetail() }
                .padding(top = 4.dp)
        )
    }
}

@Composable
private fun RecentActivityCard(state: WorkerHistoryState) {
    val hasActivity = state.recentAttendance.isNotEmpty() ||
        state.recentProduction.isNotEmpty() ||
        state.recentAdvances.isNotEmpty()

    HistoryCard(title = "Recent Activity", icon = Icons.Outlined.Schedule) {
        if (!hasActivity) {
            Text("No activity recorded yet", color = TextTertiary, fontSize = 13.sp)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                // Merge all recent items sorted by date desc
                val items: List<ActivityItem> = buildList {
                    state.recentAttendance.forEach { rec ->
                        add(ActivityItem(
                            date = rec.date,
                            icon = if (rec.isPresent) Icons.Outlined.CheckCircle else Icons.Outlined.MoneyOff,
                            label = if (rec.isPresent) "Present" else "Absent",
                            detail = DateUtils.formatDay(rec.date),
                            color = if (rec.isPresent) Success else ErrorRed
                        ))
                    }
                    state.recentProduction.forEach { entry ->
                        add(ActivityItem(
                            date = entry.date,
                            icon = Icons.Outlined.Construction,
                            label = "Production",
                            detail = "${entry.quantityProduced - entry.defectiveQuantity} pcs · ${DateUtils.formatDay(entry.date)}",
                            color = InfoBlue
                        ))
                    }
                    state.recentAdvances.forEach { adv ->
                        add(ActivityItem(
                            date = adv.date,
                            icon = Icons.Outlined.AttachMoney,
                            label = "Advance",
                            detail = "${Money.formatRupees(adv.amount)} · ${DateUtils.formatDay(adv.date)}",
                            color = WarningAmber
                        ))
                    }
                }.sortedByDescending { it.date }.take(10)

                items.forEachIndexed { index, item ->
                    ActivityRow(item)
                    if (index < items.lastIndex) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(0.5.dp)
                                .background(OverlayWhite07)
                        )
                    }
                }
            }
        }
    }
}

// ── Private data class for unified activity list ──────────────────────────────

private data class ActivityItem(
    val date: Long,
    val icon: ImageVector,
    val label: String,
    val detail: String,
    val color: androidx.compose.ui.graphics.Color
)

// ── Reusable sub-composables ─────────────────────────────────────────────────

@Composable
private fun HistoryCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceCard)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
            Text(title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
        content()
    }
}

@Composable
private fun KpiItem(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(label, color = TextSecondary, fontSize = 11.sp)
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    highlight: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            color = if (highlight) TextPrimary else TextSecondary,
            fontSize = if (highlight) 14.sp else 13.sp,
            fontWeight = if (highlight) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun HorizontalDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(OverlayWhite07)
    )
}

@Composable
private fun ActivityRow(item: ActivityItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(item.color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(item.icon, contentDescription = null, tint = item.color, modifier = Modifier.size(16.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(item.label, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.W500)
            Text(item.detail, color = TextSecondary, fontSize = 11.sp)
        }
    }
}
