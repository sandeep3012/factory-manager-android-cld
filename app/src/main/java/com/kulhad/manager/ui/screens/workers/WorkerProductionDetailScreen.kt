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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.kulhad.manager.data.repository.ProductionRepository
import com.kulhad.manager.data.repository.WorkerRepository
import com.kulhad.manager.data.util.DateUtils
import com.kulhad.manager.data.util.Money
import com.kulhad.manager.domain.model.ProductionEntry
import com.kulhad.manager.ui.components.KpiStrip
import com.kulhad.manager.ui.components.KulhadTopBar
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
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class WorkerProductionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val productionRepository: ProductionRepository,
    private val workerRepository: WorkerRepository
) : ViewModel() {

    private val workerId: Long = checkNotNull(savedStateHandle["workerId"])

    private val _selectedMonth = MutableStateFlow(DateUtils.startOfMonth(System.currentTimeMillis()))
    val selectedMonth: StateFlow<Long> = _selectedMonth.asStateFlow()

    val workerName: StateFlow<String> = flow {
        emit(workerRepository.getWorker(workerId)?.name ?: "Worker")
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "Worker")

    @OptIn(ExperimentalCoroutinesApi::class)
    val entries: StateFlow<List<ProductionEntry>> = _selectedMonth.flatMapLatest { anchor ->
        val from = DateUtils.startOfMonth(anchor)
        val to   = DateUtils.endOfMonth(anchor)
        productionRepository.observeEntriesForWorkerInRange(workerId, from, to)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun prevMonth() {
        _selectedMonth.value = DateUtils.addMonths(_selectedMonth.value, -1)
    }

    fun nextMonth() {
        val next = DateUtils.addMonths(_selectedMonth.value, 1)
        val now  = DateUtils.startOfMonth(System.currentTimeMillis())
        if (next <= now) _selectedMonth.value = next
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun WorkerProductionDetailScreen(
    onBack: () -> Unit,
    viewModel: WorkerProductionDetailViewModel = hiltViewModel()
) {
    val workerName    by viewModel.workerName.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val entries       by viewModel.entries.collectAsStateWithLifecycle()

    val netQty   = entries.sumOf { it.quantityProduced - it.defectiveQuantity }
    val defective = entries.sumOf { it.defectiveQuantity }
    val earnings  = entries.sumOf { (it.quantityProduced - it.defectiveQuantity) * it.rateSnapshot }

    val isCurrentMonth = DateUtils.startOfMonth(System.currentTimeMillis()) == selectedMonth

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
    ) {
        KulhadTopBar(
            title    = "Production History",
            subtitle = workerName,
            onBack   = onBack
        )

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ── Month picker ──────────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceCard)
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(OverlayWhite07)
                            .clickable { viewModel.prevMonth() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.ChevronLeft,
                            contentDescription = "Previous month",
                            tint = PrimaryBlue,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Text(
                        text       = DateUtils.formatMonth(selectedMonth),
                        color      = TextPrimary,
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.W600
                    )

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(OverlayWhite07)
                            .alpha(if (isCurrentMonth) 0.3f else 1f)
                            .clickable(enabled = !isCurrentMonth) { viewModel.nextMonth() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.ChevronRight,
                            contentDescription = "Next month",
                            tint = PrimaryBlue,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // ── KPI strip ─────────────────────────────────────────────────────
            item {
                KpiStrip(
                    items = listOf(
                        Triple(netQty.toString(),                    "Good Qty",  Success),
                        Triple(defective.toString(),                 "Defective", ErrorRed),
                        Triple(Money.formatRupees(earnings.toInt()), "Earnings",  InfoBlue)
                    )
                )
            }

            // ── Entry list ────────────────────────────────────────────────────
            if (entries.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceCard)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No production entries for this month",
                            color = TextTertiary,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                items(entries, key = { it.id }) { entry ->
                    ProductionEntryRow(entry)
                }
            }
        }
    }
}

// ── Entry row card ────────────────────────────────────────────────────────────

@Composable
private fun ProductionEntryRow(entry: ProductionEntry) {
    val net    = entry.quantityProduced - entry.defectiveQuantity
    val earned = net * entry.rateSnapshot

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceCard)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text       = entry.productLabel,
                color      = TextPrimary,
                fontSize   = 15.sp,
                fontWeight = FontWeight.W600
            )
            Text(
                text     = DateUtils.formatDayShort(entry.date),
                color    = TextSecondary,
                fontSize = 12.sp
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Good qty",  color = TextSecondary, fontSize = 11.sp)
                Text(
                    text       = net.toString(),
                    color      = Success,
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.W600
                )
            }
            if (entry.defectiveQuantity > 0) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text("Defective", color = TextSecondary, fontSize = 11.sp)
                    Text(
                        text       = entry.defectiveQuantity.toString(),
                        color      = ErrorRed,
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.W600
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text("Earnings", color = TextSecondary, fontSize = 11.sp)
                Text(
                    text       = Money.formatRupees(earned.toInt()),
                    color      = InfoBlue,
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.W600
                )
            }
        }

        if (entry.rateSnapshot > 0.0) {
            Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(OverlayWhite07))
            Text(
                text     = "Rate: ₹${"%.2f".format(entry.rateSnapshot)}/pc · Produced: ${entry.quantityProduced}",
                color    = TextTertiary,
                fontSize = 11.sp
            )
        }
    }
}
