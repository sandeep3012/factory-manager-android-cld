package com.kulhad.manager.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AssignmentInd
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Inventory
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.ui.graphics.vector.ImageVector

/** Global route constants for the NavHost. */
object Routes {
    const val LOGIN = "login"

    const val DASHBOARD = "dashboard"
    const val WORKERS = "workers"
    const val PRODUCTION = "production"
    const val SALES = "sales"
    const val STOCK = "stock"

    const val ADD_WORKER = "add_worker"          // ?workerId={workerId}
    const val WORKER_TYPE_HISTORY = "worker_type_history" // /{workerId}
    const val ATTENDANCE = "attendance"
    const val ATTENDANCE_HISTORY = "attendance_history"
    const val ADVANCE_ENTRY = "advance_entry"    // ?workerId={workerId}

    const val ADD_PRODUCTION = "add_production"
    const val PRODUCTION_HISTORY = "production_history"

    const val CREATE_SALE = "create_sale"
    const val PENDING_PAYMENTS = "pending_payments"
    const val PAYMENT_ENTRY = "payment_entry"    // /{saleId}

    const val STOCK_LEDGER = "stock_ledger"      // /{productId}
    const val STOCK_ADJUSTMENT = "stock_adjustment"
    const val STOCK_ADJUSTMENT_HISTORY = "stock_adjustment_history"

    const val EXPENSE = "expense"
    const val ADD_EXPENSE = "add_expense"
    const val EXPENSE_HISTORY = "expense_history"

    const val REPORTS = "reports"
    const val SALARY_REPORT = "salary_report"
    const val PROFIT_LOSS_REPORT = "profit_loss_report"
    const val PRODUCTION_REPORT = "production_report"
    const val SALES_REPORT = "sales_report"

    // ── Masters (Phase 1: Product Master) ──────────────────────────────────────
    /** Hub screen listing all master data sections. */
    const val MASTERS = "masters"
    /** Product / Kulhad-size master CRUD screen. */
    const val PRODUCT_MASTER = "product_master"

    // ── Settings ────────────────────────────────────────────────────────────────
    /** Settings hub: Data Management (backup / restore). */
    const val SETTINGS = "settings"

    fun addWorker(workerId: Long? = null): String =
        if (workerId == null) ADD_WORKER else "$ADD_WORKER?workerId=$workerId"

    fun workerTypeHistory(workerId: Long): String = "$WORKER_TYPE_HISTORY/$workerId"
    fun advanceEntry(workerId: Long? = null): String =
        if (workerId == null) ADVANCE_ENTRY else "$ADVANCE_ENTRY?workerId=$workerId"

    fun paymentEntry(saleId: Long): String = "$PAYMENT_ENTRY/$saleId"
    fun stockLedger(productId: Long): String = "$STOCK_LEDGER/$productId"
}

data class BottomTab(
    val route: String,
    val label: String,
    val icon: ImageVector
)

val BottomTabs: List<BottomTab> = listOf(
    BottomTab(Routes.DASHBOARD, "Dashboard", Icons.Outlined.GridView),
    BottomTab(Routes.WORKERS, "Workers", Icons.Outlined.AssignmentInd),
    BottomTab(Routes.PRODUCTION, "Production", Icons.Outlined.Inventory),
    BottomTab(Routes.SALES, "Sales", Icons.Outlined.Storefront),
    BottomTab(Routes.STOCK, "Stock", Icons.Outlined.Payments)
)

/** Routes where the bottom navigation bar should be visible. */
val BottomNavRoutes: Set<String> = BottomTabs.map { it.route }.toSet()
