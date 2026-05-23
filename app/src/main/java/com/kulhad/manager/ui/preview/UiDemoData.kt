package com.kulhad.manager.ui.preview

/**
 * Realistic demo/preview data used by screens to populate the UI during testing
 * when the local database is empty (fresh install).
 *
 * USAGE PATTERN (in any screen):
 *   val displayValue = if (realValue == 0L) UiDemoData.dashNetProfit else realValue
 *
 * REMOVAL: Search for "UiDemoData" to find every usage.
 * All usages are guarded by an "if real data is empty" condition,
 * so removing them (or setting SHOW_DEMO = false) leaves production logic intact.
 */
object UiDemoData {

    /** Set to false to disable all demo data overlays globally. */
    const val SHOW_DEMO = false

    // ─── Dashboard ─────────────────────────────────────────────────────────

    const val dashNetProfit    = 130_800L       // ₹1,30,800
    const val dashRevenue      = 240_000L       // ₹2,40,000
    const val dashCosts        = 109_200L       // ₹1,09,200
    val dashProduction7d       = listOf(120, 180, 95, 210, 160, 190, 145)
    val dashLabels7d           = listOf("M", "T", "W", "T", "F", "S", "S")
    const val dashPiecesToday  = 210
    const val dashSalesToday   = 18_400L
    const val dashWorkersPresent = 24
    const val dashWorkersTotal   = 28
    const val dashStockAlerts    = 2

    // ─── Workers ───────────────────────────────────────────────────────────

    /** Simple data containers for demo worker rows (no domain-model dependency). */
    data class DemoWorker(
        val name: String,
        val type: String,      // "Piece" | "Salary"
        val detail: String,
        val isPresent: Boolean
    )

    val workers = listOf(
        DemoWorker("Ramesh Kumar",   "Piece",  "182 pcs · ₹2,548", true),
        DemoWorker("Sunita Patel",   "Salary", "₹500/day",          true),
        DemoWorker("Mohan Kashyap", "Piece",  "0 pcs — absent",   false),
        DemoWorker("Priya Devi",    "Piece",  "204 pcs · ₹2,856", true),
    )

    const val workerTotal   = 28
    const val workerPresent = 24
    const val workerAbsent  = 4

    // ─── Production ────────────────────────────────────────────────────────

    val productionDaily  = listOf(320, 480, 260, 550, 410, 490, 360)
    val productionLabels = listOf("M", "T", "W", "T", "F", "S", "S")
    const val productionTotal7d = 2_870
    const val productionDefective = 143
    const val productionQuality   = 95        // percent

    /** Rate-table demo rows: sizeMl to rate (paise). */
    val pieceRates = listOf(
        60 to 1.20, 80 to 1.20, 100 to 1.20, 120 to 1.20,
        150 to 1.20, 175 to 1.20, 200 to 1.20, 250 to 1.20
    )

    // ─── Sales ─────────────────────────────────────────────────────────────

    const val salesWeekTotal   = 240_000L
    const val salesOrderCount  = 38
    const val salesCollected   = 196_200L
    const val salesPending     = 43_800L
    val salesDaily7d           = listOf(32_000f, 41_000f, 28_000f, 55_000f, 38_000f, 48_000f, 31_000f)

    data class DemoSale(
        val customer: String,
        val meta: String,
        val amount: String,
        val status: String      // "Paid" | "Partial" | "Unpaid"
    )

    val recentSales = listOf(
        DemoSale("Sharma Traders",   "Wholesale · 10 May", "₹18,400", "Paid"),
        DemoSale("Walk-in Customer", "Direct · Cash · 10 May", "₹3,200", "Paid"),
        DemoSale("Gupta Enterprises","Wholesale · 9 May",  "₹42,000", "Partial"),
        DemoSale("Ram Chai Stall",   "Retail · 9 May",     "₹1,800",  "Unpaid"),
        DemoSale("Patel & Sons",     "Wholesale · 8 May",  "₹28,600", "Paid"),
    )

    // ─── Stock ─────────────────────────────────────────────────────────────

    data class DemoStock(val sizeMl: Int, val qty: Int)

    val stockItems = listOf(
        DemoStock(60,  1_240), DemoStock(80,  856),
        DemoStock(100, 2_100), DemoStock(120, 480),
        DemoStock(150, 320),  DemoStock(175, 75),
        DemoStock(200, 1_680), DemoStock(250, 540)
    )

    // ─── Expenses ──────────────────────────────────────────────────────────

    const val expenseTotal     = 109_200L
    const val expenseLabor     = 84_200L
    const val expenseSoil      = 18_000L
    const val expenseTransport = 7_000L

    val expenseBreakdown = listOf(
        "Labor" to 84_200L,
        "Soil"  to 18_000L,
        "Transport" to 7_000L,
    )

    data class DemoExpense(val amount: String, val type: String, val remark: String, val date: String)

    val recentExpenses = listOf(
        DemoExpense("₹12,000", "Labor",     "Weekly wages",        "10 May"),
        DemoExpense("₹3,500",  "Soil",      "Red soil — 5 bags",  "9 May"),
        DemoExpense("₹1,200",  "Transport", "Goods delivery",      "8 May"),
        DemoExpense("₹8,400",  "Labor",     "Overtime payout",     "7 May"),
        DemoExpense("₹2,000",  "Soil",      "Clay material",       "6 May"),
    )

    // ─── P&L Report ────────────────────────────────────────────────────────

    const val plNetProfit   = 130_800L
    const val plRevenue     = 240_000L
    const val plLaborCost   =  84_200L
    const val plOtherCosts  =  25_000L
    val plMonthlyTrend      = listOf(95_000f, 112_000f, 108_000f, 130_800f)
    val plTrendLabels       = listOf("Feb", "Mar", "Apr", "May")
    const val plPercentChange = 12.0

    // ─── Salary Report ─────────────────────────────────────────────────────

    data class DemoSalaryRow(
        val name: String,
        val type: String,         // "Piece" | "Salary"
        val computation: String,
        val amount: String
    )

    val salaryRows = listOf(
        DemoSalaryRow("Priya Devi",    "Piece",  "204 pcs × ₹14",     "₹2,856"),
        DemoSalaryRow("Ramesh Kumar",  "Piece",  "182 pcs × ₹14",     "₹2,548"),
        DemoSalaryRow("Sunita Patel",  "Salary", "26 days × ₹500",    "₹13,000"),
        DemoSalaryRow("Mohan Kashyap","Piece",  "130 pcs × ₹14",     "₹1,820"),
        DemoSalaryRow("Raj Verma",     "Piece",  "118 pcs × ₹14",     "₹1,652"),
    )

    const val salaryTotalPayout   = 84_200L
    const val salaryTotalAdvances =  6_500L

    // ─── Production Report ─────────────────────────────────────────────────

    val prodBySize    = listOf(450f, 680f, 920f, 380f, 210f, 150f, 580f, 340f)
    val prodSizeLabels= listOf("60", "80", "100","120","150","175","200","250")

    data class DemoTopWorker(val name: String, val pieces: Float)
    val topWorkers = listOf(
        DemoTopWorker("Priya Devi",    204f),
        DemoTopWorker("Ramesh Kumar",  182f),
        DemoTopWorker("Sunita Patel",  156f),
        DemoTopWorker("Mohan Kashyap",130f),
        DemoTopWorker("Raj Verma",     118f),
    )

    // ─── Sales Report ──────────────────────────────────────────────────────

    val salesReportDaily = listOf(32_000f, 41_000f, 28_000f, 55_000f, 38_000f, 48_000f, 31_000f)

    data class DemoTopCustomer(val name: String, val total: String, val orders: Int)
    val topCustomers = listOf(
        DemoTopCustomer("Gupta Enterprises", "₹1,24,000", 8),
        DemoTopCustomer("Sharma Traders",    "₹84,200",   6),
        DemoTopCustomer("Patel & Sons",      "₹42,600",   4),
        DemoTopCustomer("Walk-in",           "₹15,800",  12),
    )
}
