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
import com.kulhad.manager.data.local.entity.WorkerType
import com.kulhad.manager.data.util.DateUtils
import com.kulhad.manager.domain.model.DailyProduction
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
    private val productDao: ProductDao
) {

    suspend fun profitLossForMonth(monthAnchor: Long): ProfitLossReport {
        val from = DateUtils.startOfMonth(monthAnchor)
        val to = DateUtils.endOfMonth(monthAnchor)

        val totalSales = saleDao.observeTotalInRange(from, to).first()
        val laborCost = productionDao.observeLaborCostInRange(from, to).first().toInt()

        val types = expenseTypeDao.observeActive().first().associate { it.id to it.name }
        val breakdown = expenseDao.observeBreakdownInRange(from, to).first()
        val expensesByType = breakdown.map { (types[it.typeId] ?: "Other") to it.amount }
        val totalExpenses = breakdown.sumOf { it.amount }

        val netProfit = totalSales - laborCost - totalExpenses

        val prevAnchor = DateUtils.addMonths(monthAnchor, -1)
        val prevFrom = DateUtils.startOfMonth(prevAnchor)
        val prevTo = DateUtils.endOfMonth(prevAnchor)
        val prevSales = saleDao.observeTotalInRange(prevFrom, prevTo).first()
        val prevLabor = productionDao.observeLaborCostInRange(prevFrom, prevTo).first().toInt()
        val prevExpenses = expenseDao.observeTotalInRange(prevFrom, prevTo).first()
        val prevProfit = prevSales - prevLabor - prevExpenses

        val percentChange = if (prevProfit == 0) 0.0
        else ((netProfit - prevProfit).toDouble() / kotlin.math.abs(prevProfit)) * 100.0

        // Last 4 months trend
        val trend = (3 downTo 0).map { offset ->
            val anchor = DateUtils.addMonths(monthAnchor, -offset)
            val mFrom = DateUtils.startOfMonth(anchor)
            val mTo = DateUtils.endOfMonth(anchor)
            val s = saleDao.observeTotalInRange(mFrom, mTo).first()
            val l = productionDao.observeLaborCostInRange(mFrom, mTo).first().toInt()
            val e = expenseDao.observeTotalInRange(mFrom, mTo).first()
            DateUtils.formatMonth(anchor) to (s - l - e)
        }

        return ProfitLossReport(
            periodLabel = DateUtils.formatMonth(monthAnchor),
            totalSales = totalSales,
            laborCost = laborCost,
            expenseByType = expensesByType,
            totalExpenses = totalExpenses,
            netProfit = netProfit,
            previousProfit = prevProfit,
            percentChange = percentChange,
            monthlyTrend = trend
        )
    }

    suspend fun salaryReportForMonth(monthAnchor: Long): SalaryReport {
        val from = DateUtils.startOfMonth(monthAnchor)
        val to = DateUtils.endOfMonth(monthAnchor)

        val workers = workerDao.observeAll().first()
        val rows = workers.map { w ->
            val type = runCatching { WorkerType.valueOf(w.currentType) }.getOrDefault(WorkerType.PIECE)
            val advances = advanceDao.totalForWorkerInRange(w.id, from, to)

            val pieceQty = productionDao.netQtyForWorkerInRange(w.id, from, to)
            val pieceEarnings = productionDao.earningsForWorkerInRange(w.id, from, to)

            val daysPresent = attendanceDao.countPresentInRange(w.id, from, to)
            val salaryEarnings = daysPresent * w.dailyRate

            val gross = if (type == WorkerType.PIECE) pieceEarnings.toInt() else salaryEarnings
            val net = gross - advances

            WorkerSalaryRow(
                workerId = w.id,
                workerName = w.name,
                workerType = type,
                pieceQty = pieceQty,
                pieceEarnings = pieceEarnings,
                daysPresent = daysPresent,
                dailyRate = w.dailyRate,
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
