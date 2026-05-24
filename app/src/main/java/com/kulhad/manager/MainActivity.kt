package com.kulhad.manager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kulhad.manager.ui.components.BottomNavBar
import com.kulhad.manager.ui.navigation.BottomNavRoutes
import com.kulhad.manager.ui.navigation.BottomTabs
import com.kulhad.manager.ui.navigation.Routes
import com.kulhad.manager.ui.screens.auth.LoginScreen
import com.kulhad.manager.ui.screens.dashboard.DashboardScreen
import com.kulhad.manager.ui.screens.expense.AddExpenseScreen
import com.kulhad.manager.ui.screens.expense.ExpenseScreen
import com.kulhad.manager.ui.screens.production.AddProductionScreen
import com.kulhad.manager.ui.screens.production.ProductionHistoryScreen
import com.kulhad.manager.ui.screens.production.ProductionScreen
import com.kulhad.manager.ui.screens.reports.ProfitLossReportScreen
import com.kulhad.manager.ui.screens.reports.ProductionReportScreen
import com.kulhad.manager.ui.screens.reports.ReportsScreen
import com.kulhad.manager.ui.screens.reports.SalaryReportScreen
import com.kulhad.manager.ui.screens.reports.SalesReportScreen
import com.kulhad.manager.ui.screens.sales.CreateSaleScreen
import com.kulhad.manager.ui.screens.sales.PaymentEntryScreen
import com.kulhad.manager.ui.screens.sales.PendingPaymentsScreen
import com.kulhad.manager.ui.screens.sales.SalesScreen
import com.kulhad.manager.ui.screens.stock.StockAdjustmentScreen
import com.kulhad.manager.ui.screens.stock.StockLedgerScreen
import com.kulhad.manager.ui.screens.stock.StockScreen
import com.kulhad.manager.ui.screens.workers.AddWorkerScreen
import com.kulhad.manager.ui.screens.workers.AdvanceEntryScreen
import com.kulhad.manager.ui.screens.workers.AttendanceHistoryScreen
import com.kulhad.manager.ui.screens.workers.AttendanceScreen
import com.kulhad.manager.ui.screens.workers.WorkerListScreen
import com.kulhad.manager.ui.screens.workers.WorkerTypeHistoryScreen
import com.kulhad.manager.ui.theme.KulhadTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KulhadTheme {
                val navController = rememberNavController()
                val backEntry by navController.currentBackStackEntryAsState()
                val currentRoute = backEntry?.destination?.route
                val showBottomBar = currentRoute in BottomNavRoutes

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (showBottomBar) {
                            BottomNavBar(
                                tabs = BottomTabs,
                                currentRoute = currentRoute ?: "",
                                onTabSelected = { route ->
                                    navController.navigate(route) {
                                        popUpTo(Routes.DASHBOARD) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = Routes.LOGIN
                        ) {
                            // ── Auth ──────────────────────────────────────────────────────────────
                            composable(Routes.LOGIN) {
                                LoginScreen(
                                    onLoggedIn = {
                                        navController.navigate(Routes.DASHBOARD) {
                                            popUpTo(Routes.LOGIN) { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    }
                                )
                            }

                            // ── Dashboard ────────────────────────────────────────────────────────
                            composable(Routes.DASHBOARD) {
                                DashboardScreen(
                                    onAttendance = { navController.navigate(Routes.ATTENDANCE) },
                                    onAddProduction = { navController.navigate(Routes.ADD_PRODUCTION) },
                                    onCreateSale = { navController.navigate(Routes.CREATE_SALE) },
                                    onAddExpense = { navController.navigate(Routes.ADD_EXPENSE) },
                                    onOpenReports = { navController.navigate(Routes.REPORTS) },
                                    onOpenStock = { navController.navigate(Routes.STOCK) }
                                )
                            }

                            // ── Workers ──────────────────────────────────────────────────────────
                            composable(Routes.WORKERS) {
                                WorkerListScreen(
                                    onAddWorker = { navController.navigate(Routes.addWorker()) },
                                    onEditWorker = { id -> navController.navigate(Routes.addWorker(id)) },
                                    onTypeHistory = { id -> navController.navigate(Routes.workerTypeHistory(id)) },
                                    onAttendance = { navController.navigate(Routes.ATTENDANCE) },
                                    onAdvanceEntry = { navController.navigate(Routes.ADVANCE_ENTRY) }
                                )
                            }

                            composable(
                                route = "${Routes.ADD_WORKER}?workerId={workerId}",
                                arguments = listOf(
                                    navArgument("workerId") {
                                        type = NavType.LongType
                                        defaultValue = -1L
                                    }
                                )
                            ) { backStack ->
                                val workerId = backStack.arguments?.getLong("workerId")
                                    ?.takeIf { it != -1L }
                                AddWorkerScreen(
                                    workerId = workerId,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(
                                route = "${Routes.WORKER_TYPE_HISTORY}/{workerId}",
                                arguments = listOf(
                                    navArgument("workerId") { type = NavType.LongType }
                                )
                            ) { backStack ->
                                val workerId = backStack.arguments!!.getLong("workerId")
                                WorkerTypeHistoryScreen(
                                    workerId = workerId,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(Routes.ATTENDANCE) {
                                AttendanceScreen(
                                    onBack = { navController.popBackStack() },
                                    onHistory = { navController.navigate(Routes.ATTENDANCE_HISTORY) }
                                )
                            }

                            composable(Routes.ATTENDANCE_HISTORY) {
                                AttendanceHistoryScreen(onBack = { navController.popBackStack() })
                            }

                            composable(
                                route = "${Routes.ADVANCE_ENTRY}?workerId={workerId}",
                                arguments = listOf(
                                    navArgument("workerId") {
                                        type = NavType.LongType
                                        defaultValue = -1L
                                    }
                                )
                            ) { backStack ->
                                val workerId = backStack.arguments?.getLong("workerId")
                                    ?.takeIf { it != -1L }
                                AdvanceEntryScreen(
                                    initialWorkerId = workerId,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            // ── Production ───────────────────────────────────────────────────────
                            composable(Routes.PRODUCTION) {
                                ProductionScreen(
                                    onAddProduction = { navController.navigate(Routes.ADD_PRODUCTION) },
                                    onHistory = { navController.navigate(Routes.PRODUCTION_HISTORY) }
                                )
                            }

                            composable(Routes.ADD_PRODUCTION) {
                                AddProductionScreen(onBack = { navController.popBackStack() })
                            }

                            composable(Routes.PRODUCTION_HISTORY) {
                                ProductionHistoryScreen(onBack = { navController.popBackStack() })
                            }

                            // ── Sales ────────────────────────────────────────────────────────────
                            composable(Routes.SALES) {
                                SalesScreen(
                                    onCreateSale = { navController.navigate(Routes.CREATE_SALE) },
                                    onPendingPayments = { navController.navigate(Routes.PENDING_PAYMENTS) },
                                    onSaleClick = { id -> navController.navigate(Routes.paymentEntry(id)) }
                                )
                            }

                            composable(Routes.CREATE_SALE) {
                                CreateSaleScreen(onBack = { navController.popBackStack() })
                            }

                            composable(Routes.PENDING_PAYMENTS) {
                                PendingPaymentsScreen(
                                    onBack = { navController.popBackStack() },
                                    onSaleClick = { id -> navController.navigate(Routes.paymentEntry(id)) }
                                )
                            }

                            composable(
                                route = "${Routes.PAYMENT_ENTRY}/{saleId}",
                                arguments = listOf(
                                    navArgument("saleId") { type = NavType.LongType }
                                )
                            ) { backStack ->
                                val saleId = backStack.arguments!!.getLong("saleId")
                                PaymentEntryScreen(
                                    saleId = saleId,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            // ── Stock ────────────────────────────────────────────────────────────
                            composable(Routes.STOCK) {
                                StockScreen(
                                    onAdjust = { navController.navigate(Routes.STOCK_ADJUSTMENT) },
                                    onLedger = { id -> navController.navigate(Routes.stockLedger(id)) }
                                )
                            }

                            composable(
                                route = "${Routes.STOCK_LEDGER}/{productId}",
                                arguments = listOf(
                                    navArgument("productId") { type = NavType.LongType }
                                )
                            ) { backStack ->
                                val productId = backStack.arguments!!.getLong("productId")
                                StockLedgerScreen(
                                    productId = productId,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(Routes.STOCK_ADJUSTMENT) {
                                StockAdjustmentScreen(onBack = { navController.popBackStack() })
                            }

                            // ── Expense ──────────────────────────────────────────────────────────
                            composable(Routes.EXPENSE) {
                                ExpenseScreen(
                                    onAddExpense = { navController.navigate(Routes.ADD_EXPENSE) },
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(Routes.ADD_EXPENSE) {
                                AddExpenseScreen(onBack = { navController.popBackStack() })
                            }

                            // ── Reports ──────────────────────────────────────────────────────────
                            composable(Routes.REPORTS) {
                                ReportsScreen(
                                    onBack = { navController.popBackStack() },
                                    onSalary = { navController.navigate(Routes.SALARY_REPORT) },
                                    onProfitLoss = { navController.navigate(Routes.PROFIT_LOSS_REPORT) },
                                    onProduction = { navController.navigate(Routes.PRODUCTION_REPORT) },
                                    onSales = { navController.navigate(Routes.SALES_REPORT) }
                                )
                            }

                            composable(Routes.SALARY_REPORT) {
                                SalaryReportScreen(onBack = { navController.popBackStack() })
                            }

                            composable(Routes.PROFIT_LOSS_REPORT) {
                                ProfitLossReportScreen(onBack = { navController.popBackStack() })
                            }

                            composable(Routes.PRODUCTION_REPORT) {
                                ProductionReportScreen(onBack = { navController.popBackStack() })
                            }

                            composable(Routes.SALES_REPORT) {
                                SalesReportScreen(onBack = { navController.popBackStack() })
                            }
                        }
                    }
                }
            }
        }
    }
}
