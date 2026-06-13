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
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
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
    val workingDate: StateFlow<LocalDate> = workingDateManager.currentWorkingDate

    fun setWorkingDate(date: LocalDate) = workingDateManager.setWorkingDate(date)

    /** Active workers only — shown in the Add Production picker. */
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

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val historyDayEntries: StateFlow<List<ProductionEntry>> =
        workingDateManager.currentWorkingDate
            .flatMapLatest { date ->
                val epochMilli = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                productionRepository.observeEntriesForDay(epochMilli)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── Edit entry state ─────────────────────────────────────────────────────

    private val _entryForEdit = MutableStateFlow<ProductionEntry?>(null)
    /** Non-null when the edit screen is active and the entry has been loaded. */
    val entryForEdit: StateFlow<ProductionEntry?> = _entryForEdit.asStateFlow()

    /** Loads an existing entry by ID into [entryForEdit] for the edit form. */
    fun loadEntryForEdit(entryId: Long) {
        viewModelScope.launch {
            _entryForEdit.value = productionRepository.findEntryById(entryId)
        }
    }

    /** Clears the edit state — call when leaving the edit screen. */
    fun clearEntryForEdit() {
        _entryForEdit.value = null
    }

    // ── Write operations ─────────────────────────────────────────────────────

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
                    workerId  = workerId,
                    productId = productId,
                    quantity  = quantity,
                    defective = defective,
                    date      = workingDateManager.currentEpochMilli(),
                    userId    = sessionManager.currentUserId
                )
                onDone(id)
            } catch (_: Exception) {}
        }
    }

    fun updateEntry(
        entryId:   Long,
        quantity:  Int,
        defective: Int,
        onDone:    () -> Unit
    ) {
        viewModelScope.launch {
            try {
                productionRepository.updateEntry(
                    entryId   = entryId,
                    quantity  = quantity,
                    defective = defective,
                    userId    = sessionManager.currentUserId
                )
                onDone()
            } catch (_: Exception) {}
        }
    }
}
