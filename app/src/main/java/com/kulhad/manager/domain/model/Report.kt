package com.kulhad.manager.domain.model

import com.kulhad.manager.data.local.entity.WorkerType

data class ProfitLossReport(
    val periodLabel: String,
    val totalSales: Int,
    val laborCost: Int,
    val expenseByType: List<Pair<String, Int>>, // typeName -> amount
    val totalExpenses: Int,
    val netProfit: Int,
    val previousProfit: Int,
    val percentChange: Double,
    val monthlyTrend: List<Pair<String, Int>> // last 4 months: label -> profit
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
