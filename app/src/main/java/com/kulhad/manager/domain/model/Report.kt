package com.kulhad.manager.domain.model

import com.kulhad.manager.data.local.entity.WorkerType

/**
 * One month's P&L summary used in the 6-month multi-series trend chart.
 */
data class MonthSummary(
    val label: String,
    val revenue: Int,
    val labourCost: Int,
    val expenses: Int,
    val netProfit: Int
)

data class ProfitLossReport(
    val periodLabel: String,
    val totalSales: Int,
    /** Gross earnings for PIECE workers only: SUM(earningsForWorkerInRange). */
    val pieceLabourCost: Int,
    /** Gross earnings for SALARY workers only: SUM(daysPresent × dailyRate). */
    val salaryLabourCost: Int,
    /** Total labour cost = pieceLabourCost + salaryLabourCost. */
    val totalLabourCost: Int,
    /** Revenue minus total labour cost only (expenses not yet deducted). */
    val grossProfit: Int,
    val expenseByType: List<Pair<String, Int>>, // typeName -> amount
    val totalExpenses: Int,
    val netProfit: Int,
    // ── Previous month (for comparison card) ──────────────────────────────
    val previousRevenue: Int,
    val previousTotalLabourCost: Int,
    val previousExpenses: Int,
    val previousProfit: Int,
    val percentChange: Double,
    // ── 6-month multi-series trend ─────────────────────────────────────────
    /** Last 6 months in chronological order (oldest first). */
    val trendFull: List<MonthSummary>
)

data class WorkerSalaryRow(
    val workerId: Long,
    val workerName: String,
    val workerType: WorkerType,
    val pieceQty: Int,
    val pieceEarnings: Double,
    val daysPresent: Int,
    val dailyRate: Int,
    val salaryEarnings: Int,
    val advances: Int,
    val grossEarnings: Int,
    val netEarnings: Int
) {
    val displayEarnings: Int get() = if (workerType == WorkerType.PIECE) pieceEarnings.toInt() else salaryEarnings
}

data class SalaryReport(
    val periodLabel: String,
    val totalPayout: Int,
    val totalAdvances: Int,
    val rows: List<WorkerSalaryRow>
)

data class ProductionReport(
    val periodLabel: String,
    val totalPieces: Int,
    val defectivePieces: Int,
    val qualityPercent: Double,
    val bySize: List<SizeProduction>,
    val byWorker: List<WorkerProduction>,
    val daily: List<DailyProduction>
)

data class SizeProduction(val productId: Long, val sizeMl: Int, val qty: Int)
data class WorkerProduction(val workerId: Long, val workerName: String, val qty: Int)
data class DailyProduction(val day: Long, val qty: Int)

data class SalesReport(
    val periodLabel: String,
    val totalSales: Int,
    val orderCount: Int,
    val avgOrderValue: Int,
    val collected: Int,
    val pending: Int,
    val daily: List<Pair<Long, Int>>,        // day -> amount
    val topCustomers: List<Pair<String, Int>> // customer -> amount
)
