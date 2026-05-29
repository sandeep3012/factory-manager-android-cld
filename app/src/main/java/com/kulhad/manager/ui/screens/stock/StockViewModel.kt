package com.kulhad.manager.ui.screens.stock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kulhad.manager.data.local.entity.StockChangeType
import com.kulhad.manager.data.repository.ProductionRepository
import com.kulhad.manager.data.repository.StockRepository
import com.kulhad.manager.di.SessionManager
import com.kulhad.manager.di.WorkingDateManager
import com.kulhad.manager.domain.model.Product
import com.kulhad.manager.domain.model.StockItem
import com.kulhad.manager.domain.model.StockMovement
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class StockViewModel @Inject constructor(
    private val stockRepository: StockRepository,
    private val productionRepository: ProductionRepository,
    private val sessionManager: SessionManager,
    private val workingDateManager: WorkingDateManager
) : ViewModel() {

    // ── Global working date ──────────────────────────────────────────────────
    /**
     * The process-scoped working date from [WorkingDateManager].
     * Delegates the same StateFlow — no state is duplicated.
     * Stock adjustments use [workingDateManager.currentEpochMilli] as their business date.
     */
    val workingDate: StateFlow<LocalDate> = workingDateManager.currentWorkingDate

    /** Forwards date selection to [WorkingDateManager]; future dates are silently rejected. */
    fun setWorkingDate(date: LocalDate) = workingDateManager.setWorkingDate(date)

    val stockItems: StateFlow<List<StockItem>> = stockRepository.observeStockItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val products: StateFlow<List<Product>> = productionRepository.observeProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun observeLedger(productId: Long) = stockRepository.observeLedgerForProduct(productId)
    fun observeStockFor(productId: Long) = stockRepository.observeStockForProduct(productId)

    private val _runningBalance = MutableStateFlow<List<Pair<Long, Int>>>(emptyList())
    val runningBalance: StateFlow<List<Pair<Long, Int>>> = _runningBalance.asStateFlow()

    fun loadRunningBalance(productId: Long) {
        viewModelScope.launch {
            _runningBalance.value = stockRepository.runningBalanceLast7Days(productId)
        }
    }

    /**
     * LOSS and ADJUSTMENT entries for the currently selected [workingDate], newest first.
     *
     * Re-queries whenever the global working date changes via [WorkingDateManager].
     * Uses [flatMapLatest] so the previous DB subscription is cancelled on every date
     * change — stale data from a previous day cannot leak through.
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val historyDayAdjustments: StateFlow<List<StockMovement>> =
        workingDateManager.currentWorkingDate
            .flatMapLatest { date ->
                val epochMilli = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                stockRepository.observeAdjustmentsForDay(epochMilli)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun saveAdjustment(
        productId: Long,
        type: StockChangeType,
        quantity: Int,
        remark: String,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                stockRepository.addAdjustment(
                    productId      = productId,
                    quantityChange = quantity,
                    type           = type,
                    remark         = remark,
                    userId         = sessionManager.currentUserId,
                    date           = workingDateManager.currentEpochMilli()
                )
                onDone()
            } catch (_: Exception) {}
        }
    }

    /**
     * Updates the [quantityChange] and [remark] of an existing LOSS/ADJUSTMENT row.
     *
     * Re-applies the correct sign based on [type]:
     *  - LOSS: stored as -abs(newQty)
     *  - ADJUSTMENT: stored as-is (can be +/-)
     *
     * No new row is created; this is a true UPDATE that stamps auditUpdatedBy/At.
     */
    fun updateAdjustment(
        id: Long,
        type: StockChangeType,
        newQty: Int,
        newRemark: String,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                val signed = when (type) {
                    StockChangeType.LOSS -> -kotlin.math.abs(newQty)
                    else                 -> newQty
                }
                stockRepository.updateAdjustment(id, signed, newRemark)
                onDone()
            } catch (_: Exception) {}
        }
    }
}
