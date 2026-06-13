package com.kulhad.manager.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kulhad.manager.data.repository.ProfitabilityRepository
import com.kulhad.manager.data.repository.ProductionRepository
import com.kulhad.manager.data.repository.SaleRepository
import com.kulhad.manager.data.repository.StockRepository
import com.kulhad.manager.data.repository.WorkerRepository
import com.kulhad.manager.domain.model.ProfitabilitySummary
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
    val production7DayLabels: List<String>,
    // ── Today P&L ────────────────────────────────────────────────────────────
    /** Revenue collected today = SUM(sales.total_amount) for today's date range. */
    val todayRevenue: Int,
    /**
     * Labour cost today — worker-type branching:
     *   PIECE workers  → SUM((qty − defective) × rate_snapshot)
     *   SALARY workers → present_days × daily_rate
     */
    val todayLabourCost: Int,
    /** Total expenses today = SUM(expenses.amount) for today's date range. */
    val todayExpenses: Int,
    /** Net profit today = todayRevenue − todayLabourCost − todayExpenses. */
    val todayNetProfit: Int
)

sealed class DashboardUiState {
    data object Loading : DashboardUiState()
    data class Success(val data: DashboardData) : DashboardUiState()
    data class Error(val message: String) : DashboardUiState()
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val workerRepository:        WorkerRepository,
    private val productionRepository:    ProductionRepository,
    private val saleRepository:          SaleRepository,
    private val stockRepository:         StockRepository,
    private val profitabilityRepository: ProfitabilityRepository,
    private val sessionManager:          SessionManager
) : ViewModel() {

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<DashboardUiState> = run {
        val now      = System.currentTimeMillis()
        val monthFrom = DateUtils.startOfMonth(now)
        val monthTo   = DateUtils.endOfMonth(now)
        val starts    = DateUtils.last7DayStarts()
        val from7     = starts.first()
        val to7       = DateUtils.endOfDay(starts.last())

        // ── Level 1: today values (5 sources) ──────────────────────────────────
        combine(
            productionRepository.observeNetQtyToday(),   // [0] piecesToday
            profitabilityRepository.observeToday(),      // [1] ProfitabilitySummary (today)
            workerRepository.observePresentCountToday(), // [2] present
            workerRepository.observeActiveCount(),       // [3] total workers
            stockRepository.observeAlertCount()          // [4] alerts
        ) { piecesToday, todayPnL, present, total, alert ->
            arrayOf(piecesToday, todayPnL, present, total, alert)
        }.flatMapLatest { todayArr ->
            // ── Level 2: month P&L + 7-day chart (3 sources) ─────────────────
            // profitabilityRepository.observeRange() uses worker-type branching so
            // Dashboard labour cost == P&L Report labour cost for the same month.
            combine(
                profitabilityRepository.observeRange(monthFrom, monthTo),
                productionRepository.observeDailyInRange(from7, to7),
                flowOf(todayArr)
            ) { monthPnL, daily, today ->
                val byDay  = daily.associate { it.day to it.qty }
                val series = starts.map { byDay[it] ?: 0 }
                val labels = DateUtils.last7DayLabels()

                val todayPnL = today[1] as ProfitabilitySummary

                DashboardUiState.Success(
                    DashboardData(
                        greeting             = DateUtils.greeting(),
                        userName             = sessionManager.currentUserName,
                        piecesToday          = today[0] as Int,
                        // salesToday == todayRevenue (kept for backward compat)
                        salesToday           = todayPnL.revenue,
                        workersPresent       = today[2] as Int,
                        workersTotal         = today[3] as Int,
                        stockAlertCount      = today[4] as Int,
                        netProfitMonth       = monthPnL.netProfit,
                        totalRevenueMonth    = monthPnL.revenue,
                        totalCostMonth       = monthPnL.labourCost + monthPnL.expenses,
                        production7Days      = series,
                        production7DayLabels = labels,
                        todayRevenue         = todayPnL.revenue,
                        todayLabourCost      = todayPnL.labourCost,
                        todayExpenses        = todayPnL.expenses,
                        todayNetProfit       = todayPnL.netProfit
                    )
                )
            }
        }.stateIn(
            scope          = viewModelScope,
            started        = SharingStarted.WhileSubscribed(5_000),
            initialValue   = DashboardUiState.Loading
        )
    }
}
