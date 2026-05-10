package com.kulhad.manager.ui.screens.stock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kulhad.manager.data.local.entity.StockChangeType
import com.kulhad.manager.data.repository.ProductionRepository
import com.kulhad.manager.data.repository.StockRepository
import com.kulhad.manager.di.SessionManager
import com.kulhad.manager.domain.model.Product
import com.kulhad.manager.domain.model.StockItem
import com.kulhad.manager.domain.model.StockMovement
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class StockViewModel @Inject constructor(
    private val stockRepository: StockRepository,
    private val productionRepository: ProductionRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

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
                    productId = productId,
                    quantityChange = quantity,
                    type = type,
                    remark = remark,
                    userId = sessionManager.currentUserId
                )
                onDone()
            } catch (_: Exception) {}
        }
    }
}
