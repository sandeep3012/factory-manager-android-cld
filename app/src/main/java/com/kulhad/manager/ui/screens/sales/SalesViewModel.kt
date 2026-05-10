package com.kulhad.manager.ui.screens.sales

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kulhad.manager.data.repository.SaleRepository
import com.kulhad.manager.data.repository.StockRepository
import com.kulhad.manager.data.util.DateUtils
import com.kulhad.manager.di.SessionManager
import com.kulhad.manager.domain.model.Product
import com.kulhad.manager.domain.model.SaleDetail
import com.kulhad.manager.domain.model.SaleItemDraft
import com.kulhad.manager.domain.model.SaleSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SalesTabData(
    val weekTotal: Int,
    val orderCount: Int,
    val collected: Int,
    val pending: Int,
    val daily: List<Int>,
    val recent: List<SaleSummary>
)

@HiltViewModel
class SalesViewModel @Inject constructor(
    private val saleRepository: SaleRepository,
    private val stockRepository: StockRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    val tabData: StateFlow<SalesTabData> = run {
        val starts = DateUtils.last7DayStarts()
        val from = starts.first()
        val to = DateUtils.endOfDay(starts.last())

        // Group 1: 4 ints → IntArray of size 4
        val numbersFlow = combine(
            saleRepository.observeTotalInRange(from, to),
            saleRepository.observeOrderCountInRange(from, to),
            saleRepository.observeTotalCollectedAllTime(),
            saleRepository.observeTotalPending()
        ) { total, orders, collected, pending ->
            intArrayOf(total, orders, collected, pending)
        }

        combine(
            numbersFlow,
            saleRepository.observeDailySales(from, to),
            saleRepository.observeAllSummaries()
        ) { numbers, daily, all ->
            val byDay = daily.associate { it.day to it.amount }
            val series = starts.map { byDay[it] ?: 0 }
            SalesTabData(
                weekTotal = numbers[0],
                orderCount = numbers[1],
                collected = numbers[2],
                pending = numbers[3],
                daily = series,
                recent = all.take(20)
            )
        }.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5_000),
            SalesTabData(0, 0, 0, 0, emptyList(), emptyList())
        )
    }

    val pendingSummaries: StateFlow<List<SaleSummary>> = saleRepository.observePendingSummaries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun observeSaleDetail(saleId: Long) = saleRepository.observeSaleDetail(saleId)

    val products: StateFlow<List<Product>> =
        kotlinx.coroutines.flow.flow {
            // Use stockRepository.observeStockItems just to get the product list with current stock.
            stockRepository.observeStockItems().collect { items ->
                emit(items.map { it.product })
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Returns current stock for a single product. */
    fun observeStockFor(productId: Long) = stockRepository.observeStockForProduct(productId)

    fun createSale(
        customerName: String,
        date: Long,
        items: List<SaleItemDraft>,
        onDone: (Long) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val id = saleRepository.createSale(
                    customerName = customerName,
                    date = date,
                    items = items,
                    userId = sessionManager.currentUserId
                )
                onDone(id)
            } catch (_: Exception) {}
        }
    }

    fun addPayment(
        saleId: Long,
        amount: Int,
        date: Long,
        remark: String,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                saleRepository.addPayment(saleId, amount, date, remark)
                onDone()
            } catch (_: Exception) {}
        }
    }
}
