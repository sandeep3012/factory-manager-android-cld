package com.kulhad.manager.ui.screens.production

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kulhad.manager.data.repository.ProductionRepository
import com.kulhad.manager.data.repository.WorkerRepository
import com.kulhad.manager.data.util.DateUtils
import com.kulhad.manager.di.SessionManager
import com.kulhad.manager.di.WorkingDateManager
import com.kulhad.manager.domain.model.Product
import com.kulhad.manager.domain.model.ProductWithRate
import com.kulhad.manager.domain.model.ProductionEntry
import com.kulhad.manager.domain.model.Worker
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProductionStats(
    val totalPieces: Int,
    val defective: Int,
    val qualityPercent: Int,
    val labels: List<String>,
    val daily: List<Int>
)

@HiltViewModel
class ProductionViewModel @Inject constructor(
    private val productionRepository: ProductionRepository,
    private val workerRepository: WorkerRepository,
    private val sessionManager: SessionManager,
    private val workingDateManager: WorkingDateManager
) : ViewModel() {

    // ── Global working date ──────────────────────────────────────────────────
    /**
     * The process-scoped working date from [WorkingDateManager].
     * Delegates the same StateFlow — no state is duplicated.
     * Production entry saves use [workingDateManager.currentEpochMilli] internally.
     */
    val workingDate: StateFlow<LocalDate> = workingDateManager.currentWorkingDate

    /** Forwards date selection to [WorkingDateManager]; future dates are silently rejected. */
    fun setWorkingDate(date: LocalDate) = workingDateManager.setWorkingDate(date)

    val activeWorkers: StateFlow<List<Worker>> = workerRepository.observeActiveWorkers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val products: StateFlow<List<Product>> = productionRepository.observeProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val productsWithRates: StateFlow<List<ProductWithRate>> =
        productionRepository.observeProductsWithRates()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val stats: StateFlow<ProductionStats> = run {
        val starts = DateUtils.last7DayStarts()
        val from = starts.first()
        val to = DateUtils.endOfDay(starts.last())
        combine(
            productionRepository.observeTotalQtyInRange(from, to),
            productionRepository.observeDefectiveQtyInRange(from, to),
            productionRepository.observeDailyInRange(from, to)
        ) { total, defective, daily ->
            val byDay = daily.associate { it.day to it.qty }
            val series = starts.map { byDay[it] ?: 0 }
            val labels = DateUtils.last7DayLabels()
            val quality = if (total == 0) 100 else ((total - defective) * 100) / total
            ProductionStats(
                totalPieces = total - defective,
                defective = defective,
                qualityPercent = quality,
                labels = labels,
                daily = series
            )
        }.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5_000),
            ProductionStats(0, 0, 100, emptyList(), emptyList())
        )
    }

    private val _historyMonth = MutableStateFlow(System.currentTimeMillis())
    val historyMonth: StateFlow<Long> = _historyMonth.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val historyEntries: StateFlow<List<ProductionEntry>> = _historyMonth
        .flatMapLatest { anchor ->
            val from = DateUtils.startOfMonth(anchor)
            val to = DateUtils.endOfMonth(anchor)
            productionRepository.observeEntriesInRange(from, to)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setHistoryMonth(anchor: Long) { _historyMonth.value = anchor }

    suspend fun rateFor(productId: Long): Double = productionRepository.currentRate(productId)

    fun saveEntry(
        workerId: Long,
        productId: Long,
        quantity: Int,
        defective: Int,
        onDone: (Long) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val id = productionRepository.addEntry(
                    workerId = workerId,
                    productId = productId,
                    quantity = quantity,
                    defective = defective,
                    date = workingDateManager.currentEpochMilli(),
                    userId = sessionManager.currentUserId
                )
                onDone(id)
            } catch (_: Exception) {}
        }
    }
}
