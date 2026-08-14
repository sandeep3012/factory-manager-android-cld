package com.kulhad.manager.data.repository

import com.kulhad.manager.data.local.dao.AttendanceDao
import com.kulhad.manager.data.local.dao.ExpenseDao
import com.kulhad.manager.data.local.dao.ExpenseTypeDao
import com.kulhad.manager.data.local.dao.PaymentDao
import com.kulhad.manager.data.local.dao.ProductDao
import com.kulhad.manager.data.local.dao.ProductionEntryDao
import com.kulhad.manager.data.local.dao.SaleDao
import com.kulhad.manager.data.local.dao.WorkerAdvanceDao
import com.kulhad.manager.data.local.dao.WorkerDao
import com.kulhad.manager.data.local.dao.WorkerTypeHistoryDao
import com.kulhad.manager.data.local.entity.WorkerEntity
import com.kulhad.manager.data.local.entity.WorkerType
import com.kulhad.manager.data.util.DateUtils
import com.kulhad.manager.domain.model.DailyProduction
import com.kulhad.manager.domain.model.MonthSummary
import com.kulhad.manager.domain.model.ProductionReport
import com.kulhad.manager.domain.model.ProfitLossReport
import com.kulhad.manager.domain.model.SalaryReport
import com.kulhad.manager.domain.model.SalesReport
import com.kulhad.manager.domain.model.SizeProduction
import com.kulhad.manager.domain.model.WorkerProduction
import com.kulhad.manager.domain.model.WorkerSalaryRow
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

private data class LabourCostBreakdown(
    val pieceLabourCost: Int,
    val salaryLabourCost: Int,
    val totalLabourCost: Int
)

@Singleton
class ReportRepository @Inject constructor(
    private val saleDao: SaleDao,
    private val productionDao: ProductionEntryDao,
    private val expenseDao: ExpenseDao,
    private val expenseTypeDao: ExpenseTypeDao,
    private val workerDao: WorkerDao,
    private val attendanceDao: AttendanceDao,
    private val advanceDao: WorkerAdvanceDao,
    private val paymentDao: PaymentDao,
    private val productDao: ProductDao,
    private val typeHistoryDao: WorkerTypeHistoryDao
) {

    /**
     * Resolves the worker type + daily rate that were actually in effect on [at],
     * using [WorkerTypeHistoryDao.typeAt] rather than the live [WorkerEntity.currentType] /
     * [WorkerEntity.dailyRate] cache — so date-bounded reports stay correct after a worker's
     * type/rate is later changed. Falls back to the cache if no history row exists
     * (should not normally happen — every worker gets one on creation).
     *
     * Note: this resolves a single snapshot for the whole period (as of [at]). A worker
     * who changes type *mid-period* is not split across sub-periods — a known, accepted
     * simplification.
     */
    private suspend fun resolveTypeAndRate(worker: WorkerEntity, at: Long): Pair<WorkerType, Int> {
        val history = typeHistoryDao.typeAt(worker.id, at)
        return if (history != null) {
            val type = runCatching { WorkerType.valueOf(history.workerType) }.getOrDefault(WorkerType.PIECE)
            type to history.dailyRate
        } else {
            val type = runCatching { WorkerType.valueOf(worker.currentType) }.getOrDefault(WorkerType.PIECE)
            type to worker.dailyRate
        }
    }

    /**
     * Gross labour cost for a date range, split by worker type.
     *
     * PIECE workers: earnings come from production output only.
     *   → productionDao.earningsForWorkerInRange(workerId, from, to)
     *
     * SALARY workers: earnings come from attendance only.
     *   → attendanceDao.countPresentInRange(workerId, from, to) × daily rate effective on [to]
     *   Production entries submitted by salary workers are intentionally ignored here —
     *   they count for stock and analytics but must not contribute to labour cost.
     *
     * Uses gross earnings (before advance deduction) because advances are a separate
     * cash-flow item, not a period operating cost.
     */
    private suspend fun labourCostsForRange(from: Long, to: Long): LabourCostBreakdown {
        val workers = workerDao.observeAll().first()
        var piece = 0
        var salary = 0
        workers.forEach { w ->
            val (type, dailyRate) = resolveTypeAndRate(w, to)
            if (type == WorkerType.PIECE) {
                piece += productionDao.earningsForWorkerInRange(w.id, from, to).toInt()
            } else {
                // SALARY worker — use attendance × daily rate, NOT production earnings
                val daysPresent = attendanceDao.countPresentInRange(w.id, from, to)
                salary += daysPresent * dailyRate
            }
        }
        return LabourCostBreakdown(
            pieceLabourCost  = piece,
            salaryLabourCost = salary,
            totalLabourCost  = piece + salary
        )
    }

    suspend fun profitLossForMonth(monthAnchor: Long): ProfitLossReport {
        val from = DateUtils.startOfMonth(monthAnchor)
        val to = DateUtils.endOfMonth(monthAnchor)

        val totalSales = saleDao.observeTotalInRange(from, to).first()
        val labour = labourCostsForRange(from, to)

        val types = expenseTypeDao.observeActive().first().associate { it.id to it.name }
        val breakdown = expenseDao.observeBreakdownInRange(from, to).first()
        val expensesByType = breakdown.map { (types[it.typeId] ?: "Other") to it.amount }
        val totalExpenses = breakdown.sumOf { it.amount }

        val netProfit = totalSales - labour.totalLabourCost - totalExpenses

        val prevAnchor = DateUtils.addMonths(monthAnchor, -1)
        val prevFrom = DateUtils.startOfMonth(prevAnchor)
        val prevTo = DateUtils.endOfMonth(prevAnchor)
        val prevSales = saleDao.observeTotalInRange(prevFrom, prevTo).first()
        val prevLabour = labourCostsForRange(prevFrom, prevTo)
        val prevExpenses = expenseDao.observeTotalInRange(prevFrom, prevTo).first()
        val prevProfit = prevSales - prevLabour.totalLabourCost - prevExpenses

        val percentChange = if (prevProfit == 0) 0.0
        else ((netProfit - prevProfit).toDouble() / kotlin.math.abs(prevProfit)) * 100.0

        // Last 6 months multi-series trend (oldest month first)
        val trendFull = (5 downTo 0).map { offset ->
            val anchor = DateUtils.addMonths(monthAnchor, -offset)
            val mFrom = DateUtils.startOfMonth(anchor)
            val mTo = DateUtils.endOfMonth(anchor)
            val s = saleDao.observeTotalInRange(mFrom, mTo).first()
            val mLabour = labourCostsForRange(mFrom, mTo)
            val e = expenseDao.observeTotalInRange(mFrom, mTo).first()
            MonthSummary(
                label      = DateUtils.formatMonth(anchor),
                revenue    = s,
                labourCost = mLabour.totalLabourCost,
                expenses   = e,
                netProfit  = s - mLabour.totalLabourCost - e
            )
        }

        return ProfitLossReport(
            periodLabel             = DateUtils.formatMonth(monthAnchor),
            totalSales              = totalSales,
            pieceLabourCost         = labour.pieceLabourCost,
            salaryLabourCost        = labour.salaryLabourCost,
            totalLabourCost         = labour.totalLabourCost,
            grossProfit             = totalSales - labour.totalLabourCost,
            expenseByType           = expensesByType,
            totalExpenses           = totalExpenses,
            netProfit               = netProfit,
            previousRevenue         = prevSales,
            previousTotalLabourCost = prevLabour.totalLabourCost,
            previousExpenses        = prevExpenses,
            previousProfit          = prevProfit,
            percentChange           = percentChange,
            trendFull               = trendFull
        )
    }

    suspend fun salaryReportForMonth(monthAnchor: Long): SalaryReport {
        val from = DateUtils.startOfMonth(monthAnchor)
        val to = DateUtils.endOfMonth(monthAnchor)

        val workers = workerDao.observeAll().first()
        val rows = workers.map { w ->
            val (type, dailyRate) = resolveTypeAndRate(w, to)
            val advances = advanceDao.totalForWorkerInRange(w.id, from, to)

            val pieceQty = productionDao.netQtyForWorkerInRange(w.id, from, to)
            val pieceEarnings = productionDao.earningsForWorkerInRange(w.id, from, to)

            val daysPresent = attendanceDao.countPresentInRange(w.id, from, to)
            val salaryEarnings = daysPresent * dailyRate

            val gross = if (type == WorkerType.PIECE) pieceEarnings.toInt() else salaryEarnings
            val net = gross - advances

            WorkerSalaryRow(
                workerId = w.id,
                workerName = w.name,
                workerType = type,
                pieceQty = pieceQty,
                pieceEarnings = pieceEarnings,
                daysPresent = daysPresent,
                dailyRate = dailyRate,
                salaryEarnings = salaryEarnings,
                advances = advances,
                grossEarnings = gross,
                netEarnings = net
            )
        }

        return SalaryReport(
            periodLabel = DateUtils.formatMonth(monthAnchor),
            totalPayout = rows.sumOf { it.netEarnings.coerceAtLeast(0) },
            totalAdvances = rows.sumOf { it.advances },
            rows = rows.sortedByDescending { it.grossEarnings }
        )
    }

    suspend fun productionReportForMonth(monthAnchor: Long): ProductionReport {
        val from = DateUtils.startOfMonth(monthAnchor)
        val to = DateUtils.endOfMonth(monthAnchor)

        val totalQty = productionDao.observeTotalQtyInRange(from, to).first()
        val defective = productionDao.observeDefectiveQtyInRange(from, to).first()
        val quality = if (totalQty == 0) 100.0 else ((totalQty - defective).toDouble() / totalQty) * 100.0

        val products = productDao.getAll().associateBy { it.id }
        val workers = workerDao.observeAll().first().associateBy { it.id }

        val bySize = productionDao.observeByProductInRange(from, to).first().map {
            SizeProduction(
                productId = it.productId,
                sizeMl = products[it.productId]?.sizeMl ?: 0,
                qty = it.qty
            )
        }

        val byWorker = productionDao.observeByWorkerInRange(from, to).first().map {
            WorkerProduction(
                workerId = it.workerId,
                workerName = workers[it.workerId]?.name ?: "Unknown",
                qty = it.qty
            )
        }

        val daily = productionDao.observeDailyInRange(from, to).first().map {
            DailyProduction(it.day, it.qty)
        }

        return ProductionReport(
            periodLabel = DateUtils.formatMonth(monthAnchor),
            totalPieces = totalQty - defective,
            defectivePieces = defective,
            qualityPercent = quality,
            bySize = bySize,
            byWorker = byWorker,
            daily = daily
        )
    }

    suspend fun salesReportForMonth(monthAnchor: Long): SalesReport {
        val from = DateUtils.startOfMonth(monthAnchor)
        val to = DateUtils.endOfMonth(monthAnchor)

        val total = saleDao.observeTotalInRange(from, to).first()
        val orders = saleDao.observeOrderCountInRange(from, to).first()
        val avg = if (orders == 0) 0 else total / orders
        val collected = paymentDao.observeCollectedInRange(from, to).first()
        val pending = (total - collected).coerceAtLeast(0)

        val daily = saleDao.observeDailySales(from, to).first().map { it.day to it.amount }
        val customers = saleDao.observeCustomerTotals(from, to).first().map { it.customer to it.amount }

        return SalesReport(
            periodLabel = DateUtils.formatMonth(monthAnchor),
            totalSales = total,
            orderCount = orders,
            avgOrderValue = avg,
            collected = collected,
            pending = pending,
            daily = daily,
            topCustomers = customers
        )
    }
}
