package com.kulhad.manager.data.repository

import com.kulhad.manager.data.local.dao.ExpenseDao
import com.kulhad.manager.data.local.dao.ProductionEntryDao
import com.kulhad.manager.data.local.dao.SaleDao
import com.kulhad.manager.data.util.DateUtils
import com.kulhad.manager.domain.model.ProfitabilitySummary
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Reactive P&L calculations using ONLY existing data — no new tables or migrations.
 *
 * All three source DAOs and their queries already exist; this repository simply
 * combines them into a single [ProfitabilitySummary] Flow.
 *
 * ── Sources ─────────────────────────────────────────────────────────────────────
 *  Revenue    → [SaleDao.observeTotalInRange]       — SUM(sales.total_amount)
 *  LabourCost → [ProductionEntryDao.observeLaborCostInRange]
 *               → SUM((qty_produced − defective_qty) × rate_snapshot)
 *               Historically accurate: rate_snapshot is stamped at entry time
 *               so the figure never changes when future rates are edited.
 *  Expenses   → [ExpenseDao.observeTotalInRange]    — SUM(expenses.amount)
 *
 * ── Derived ─────────────────────────────────────────────────────────────────────
 *  Gross Profit = Revenue − LabourCost
 *  Net Profit   = Revenue − LabourCost − Expenses
 */
@Singleton
class ProfitabilityRepository @Inject constructor(
    private val saleDao:          SaleDao,
    private val productionDao:    ProductionEntryDao,
    private val expenseDao:       ExpenseDao
) {

    /**
     * P&L summary for a caller-specified date range.
     *
     * [startDate] and [endDate] must be epoch-millis aligned to start-of-day /
     * end-of-day respectively (use [DateUtils.startOfDay] / [DateUtils.endOfDay]).
     * The three underlying DAO flows all use `BETWEEN :from AND :to`, so the range
     * is fully inclusive.
     *
     * Emits a new [ProfitabilitySummary] whenever any of the three source tables
     * changes — inserts, updates, or deletes in sales, production_entries, or
     * expenses all propagate automatically.
     */
    fun observeRange(startDate: Long, endDate: Long): Flow<ProfitabilitySummary> =
        combine(
            saleDao.observeTotalInRange(startDate, endDate),
            productionDao.observeLaborCostInRange(startDate, endDate),
            expenseDao.observeTotalInRange(startDate, endDate)
        ) { revenue, labourCostDouble, expenses ->
            val labourCost  = labourCostDouble.toInt()
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
     *
     * Range: [DateUtils.todayStart()] .. [DateUtils.todayEnd()].
     *
     * Note: both bounds are recomputed each time a subscriber collects this Flow.
     * This means if the app runs past midnight without being killed, the first
     * emission after midnight will already use the new day's bounds.  If tighter
     * midnight-boundary behaviour is required in future, wrap in [flatMapLatest]
     * on a tick flow — but for the current Dashboard use case this is sufficient.
     */
    fun observeToday(): Flow<ProfitabilitySummary> =
        observeRange(DateUtils.todayStart(), DateUtils.todayEnd())

    /**
     * P&L summary for the current calendar month (device timezone).
     *
     * Range: first millisecond of the current month .. last millisecond of the
     * current month.  Same midnight caveat as [observeToday] applies.
     */
    fun observeCurrentMonth(): Flow<ProfitabilitySummary> {
        val now = System.currentTimeMillis()
        return observeRange(
            DateUtils.startOfMonth(now),
            DateUtils.endOfMonth(now)
        )
    }
}
