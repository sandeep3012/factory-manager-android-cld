package com.kulhad.manager.data.repository

import com.kulhad.manager.data.local.dao.AttendanceDao
import com.kulhad.manager.data.local.dao.ExpenseDao
import com.kulhad.manager.data.local.dao.ProductionEntryDao
import com.kulhad.manager.data.local.dao.SaleDao
import com.kulhad.manager.data.local.dao.WorkerDao
import com.kulhad.manager.data.local.entity.WorkerType
import com.kulhad.manager.data.util.DateUtils
import com.kulhad.manager.domain.model.ProfitabilitySummary
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Reactive P&L calculations for the Dashboard.
 *
 * ── Labour cost ──────────────────────────────────────────────────────────────
 * Uses worker-type branching — identical business logic to
 * [ReportRepository.labourCostsForRange] — so Dashboard and P&L Report
 * always agree on labour cost for the same date range:
 *
 *   PIECE workers  → SUM((qty − defective) × rate_snapshot)   [production_entries]
 *   SALARY workers → present_days × daily_rate                 [attendance × workers]
 *
 * Production entries submitted by SALARY workers are intentionally excluded
 * from labour cost — they count for stock and production analytics only.
 *
 * ── Sources ──────────────────────────────────────────────────────────────────
 *   Revenue    → [SaleDao.observeTotalInRange]
 *   LabourCost → per-worker type branch (see above)
 *   Expenses   → [ExpenseDao.observeTotalInRange]
 */
@Singleton
class ProfitabilityRepository @Inject constructor(
    private val saleDao:          SaleDao,
    private val productionDao:    ProductionEntryDao,
    private val expenseDao:       ExpenseDao,
    private val workerDao:        WorkerDao,
    private val attendanceDao:    AttendanceDao
) {

    /**
     * Reactive labour cost for [startDate]..[endDate], branched by worker type.
     *
     * Emits whenever workers, production entries, or attendance rows change in
     * the range — any of the three source tables propagate automatically.
     */
    private fun observeLabourCost(startDate: Long, endDate: Long): Flow<Int> =
        combine(
            workerDao.observeAll(),
            productionDao.observeEarningsByWorkerInRange(startDate, endDate),
            attendanceDao.observeMonthlyWorkerCounts(startDate, endDate)
        ) { workers, productionEarnings, attendanceCounts ->
            val earningsMap = productionEarnings.associate { it.workerId to it.earnings }
            val daysMap     = attendanceCounts.associate { it.workerId to it.presentCount }
            var piece  = 0
            var salary = 0
            workers.forEach { w ->
                val type = runCatching { WorkerType.valueOf(w.currentType) }
                    .getOrDefault(WorkerType.PIECE)
                if (type == WorkerType.PIECE) {
                    piece  += (earningsMap[w.id] ?: 0.0).toInt()
                } else {
                    // SALARY worker — attendance × rate only, production entries ignored
                    salary += (daysMap[w.id] ?: 0) * w.dailyRate
                }
            }
            piece + salary
        }

    /**
     * Full P&L summary for a caller-specified date range.
     *
     * [startDate] and [endDate] must be epoch-millis aligned to start-of-day /
     * end-of-day respectively (use [DateUtils.startOfDay] / [DateUtils.endOfDay]).
     *
     * Emits a new [ProfitabilitySummary] whenever any of sales, production_entries,
     * attendance, workers, or expenses changes within the range.
     */
    fun observeRange(startDate: Long, endDate: Long): Flow<ProfitabilitySummary> =
        combine(
            saleDao.observeTotalInRange(startDate, endDate),
            expenseDao.observeTotalInRange(startDate, endDate),
            observeLabourCost(startDate, endDate)
        ) { revenue, expenses, labourCost ->
            val grossProfit = revenue - labourCost
            val netProfit   = grossProfit - expenses
            ProfitabilitySummary(
                revenue     = revenue,
                labourCost  = labourCost,
                expenses    = expenses,
                grossProfit = grossProfit,
                netProfit   = netProfit
            )
        }

    /**
     * P&L summary for the current calendar day (device timezone).
     */
    fun observeToday(): Flow<ProfitabilitySummary> =
        observeRange(DateUtils.todayStart(), DateUtils.todayEnd())

    /**
     * P&L summary for the current calendar month (device timezone).
     */
    fun observeCurrentMonth(): Flow<ProfitabilitySummary> {
        val now = System.currentTimeMillis()
        return observeRange(
            DateUtils.startOfMonth(now),
            DateUtils.endOfMonth(now)
        )
    }
}
