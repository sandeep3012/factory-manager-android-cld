package com.kulhad.manager.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kulhad.manager.data.repository.ExpenseRepository
import com.kulhad.manager.data.repository.ProductionRepository
import com.kulhad.manager.data.repository.SaleRepository
import com.kulhad.manager.data.repository.StockRepository
import com.kulhad.manager.data.repository.WorkerRepository
import com.kulhad.manager.data.util.DateUtils
import com.kulhad.manager.di.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

data class DashboardData(
    val greeting: String,
    val userName: String,
    val piecesToday: Int,
    val salesToday: Int,
    val workersPresent: Int,
    val workersTotal: Int,
    val stockAlertCount: Int,
    val netProfitMonth: Int,
    val totalRevenueMonth: Int,
    val totalCostMonth: Int,
    val production7Days: List<Int>,
    val production7DayLabels: List<String>
)

sealed class DashboardUiState {
    data object Loading : DashboardUiState()
    data class Success(val data: DashboardData) : DashboardUiState()
    data class Error(val message: String) : DashboardUiState()
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val workerRepository: WorkerRepository,
    private val productionRepository: ProductionRepository,
    private val saleRepository: SaleRepository,
    private val stockRepository: StockRepository,
    private val expenseRepository: ExpenseRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<DashboardUiState> = run {
        val now = System.currentTimeMillis()
        val monthFrom = DateUtils.startOfMonth(now)
        val monthTo = DateUtils.endOfMonth(now)
        val starts = DateUtils.last7DayStarts()
        val from7 = starts.first()
        val to7 = DateUtils.endOfDay(starts.last())

        combine(
            productionRepository.observeNetQtyToday(),
            saleRepository.observeSalesToday(),
            workerRepository.observePresentCountToday(),
            workerRepository.observeActiveCount(),
            stockRepository.observeAlertCount()
        ) { piecesToday, salesToday, present, total, alert ->
            arrayOf(piecesToday, salesToday, present, total, alert)
        }.flatMapLatest { todayArr ->
            combine(
                saleRepository.observeTotalInRange(monthFrom, monthTo),
                productionRepository.observeLaborCostInRange(monthFrom, monthTo),
                expenseRepository.observeTotalInRange(monthFrom, monthTo),
                productionRepository.observeDailyInRange(from7, to7),
                flowOf(todayArr)
            ) { revenue, labor, expenses, daily, today ->
                val byDay = daily.associate { it.day to it.qty }
                val series = starts.map { byDay[it] ?: 0 }
                val labels = DateUtils.last7DayLabels()
                val totalCost = labor.toInt() + expenses
                DashboardUiState.Success(
                    DashboardData(
                        greeting = DateUtils.greeting(),
                        userName = sessionManager.currentUserName,
                        piecesToday = today[0] as Int,
                        salesToday = today[1] as Int,
                        workersPresent = today[2] as Int,
                        workersTotal = today[3] as Int,
                        stockAlertCount = today[4] as Int,
                        netProfitMonth = revenue - totalCost,
                        totalRevenueMonth = revenue,
                        totalCostMonth = totalCost,
                        production7Days = series,
                        production7DayLabels = labels
                    )
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DashboardUiState.Loading
        )
    }
}
