# Kulhad Manager — Developer Guide

## What this app does

Android app for a 3-partner kulhad (clay cup) factory. Tracks workers (attendance, salary, advances), production (size-based output with piece/salary pay), inventory (stock ledger as single source of truth), sales (with credit/udhaar support), expenses, and profit reports.

- **Package:** `com.kulhad.manager`
- **Min SDK:** 26  |  **Compile SDK:** 35
- **Language:** Kotlin 2.0 + Jetpack Compose (Material 3)

---

## How to build

```bash
cd "D:\Sandeep\Kulhad Factory\KulhadManager"
./gradlew assembleDebug          # build
./gradlew installDebug           # install on connected device/emulator
```

Open in **Android Studio Hedgehog or later** (AGP 8.5 compatible).

### Demo login
- Email: `owner@kulhad.com`
- Password: `kulhad123`

---

## Architecture

```
app/src/main/java/com/kulhad/manager/
├── data/
│   ├── local/
│   │   ├── dao/          ← 14 Room DAOs (Flow reads, suspend writes)
│   │   ├── entity/       ← 14 @Entity classes + WorkerType / StockChangeType enums
│   │   ├── Converters.kt
│   │   └── KulhadDatabase.kt   ← seed callback: products, rates, expense types, demo user
│   ├── repository/       ← 6 repositories; critical transactions live here
│   └── util/             ← DateUtils, Money, PasswordHasher, StockThresholds
├── di/
│   ├── DatabaseModule.kt ← @Provides @Singleton: DB, all DAOs, all repositories
│   └── SessionModule.kt  ← SessionManager singleton (currentUserId, currentUserName)
├── domain/model/         ← pure data classes: Worker, Sale, Stock, Report models, etc.
├── ui/
│   ├── charts/           ← SimpleBarChart, SimpleLineChart, DonutChart, MultiSegmentDonut,
│   │                        HorizontalBarChart (all Canvas-based, no Vico)
│   ├── components/       ← KulhadTopBar, StatCard, StatusBadge, WorkerAvatar, KulhadButton,
│   │                        KulhadTextField, SizePillGrid, BottomNavBar, SegmentedControl, …
│   ├── navigation/       ← Routes object, BottomTabs list, BottomNavRoutes set
│   ├── screens/          ← 23 screens in 8 packages (auth/dashboard/workers/production/
│   │                        sales/stock/expense/reports)
│   └── theme/            ← Color.kt, Type.kt, Theme.kt (dark-first)
├── KulhadApp.kt          ← @HiltAndroidApp
└── MainActivity.kt       ← @AndroidEntryPoint, NavHost for all 23 routes, Scaffold+BottomNav
```

**Pattern:** MVVM. Every screen gets a `@HiltViewModel`. Repository methods return `Flow<T>` for reads; `suspend` functions for writes.

---

## Key design decisions

### Stock ledger as single source of truth
`stock_ledger` table stores every quantity change (PRODUCTION_IN, SALE_OUT, ADJUSTMENT, LOSS). Current stock = `SUM(quantity_change)` for a given product. Never update a stock counter column directly.

### Worker pay types
- **PIECE:** earnings = `quantity_produced - defective_quantity` × `rate_snapshot` (rate stored at entry time from `piece_rates` table).
- **SALARY:** earnings = `days_present` (from `attendance` table) × `daily_rate` (from `worker_type_history`).

### Rate snapshot
`production_entries.rate_snapshot` stores the piece rate at the time of entry so historical earnings are immutable even if rates change later.

### Auto-attendance
When a production entry is saved, the same DB transaction ensures an `attendance` row with `is_present = true` exists for that worker+day. If the row already exists, it is left unchanged.

### Salary/type history
`workers.current_type` and `workers.daily_rate` are a **cache** of the latest `worker_type_history` row. Every type or rate change writes a new history row AND updates the cache — in one transaction. Use history rows for date-bounded salary reports; use the workers cache for list screens.

### Pending payments
`pending = sale.total_amount - SUM(payments.amount)`. No payments → pending = total. Badges: **Paid** (pending=0), **Partial** (0 < paid < total), **Unpaid** (paid=0).

### Password hashing
SHA-256 (see `data/util/PasswordHasher.kt`). Acknowledged as not production-grade; follows spec.

### Date storage
All date columns store start-of-day epoch millis in the device default zone. Use `DateUtils.startOfDay()` / `DateUtils.endOfDay()` for range queries.

### Indian number formatting
`Money.formatRupees()` uses `NumberFormat.getInstance(Locale("en", "IN"))` → e.g. ₹1,23,456.

### Stock thresholds
`StockThresholds.CRITICAL = 100`, `LOW = 500`. Used by Stock screen colour coding and Dashboard alert count.

---

## Common coding patterns

### combine() with more than 4 sources of different types
Room DAOs return typed Flows; Kotlin's `combine` works up to 5 arguments but loses type safety above 4. Group Int-valued flows into an intermediate array flow:

```kotlin
val numbersFlow = combine(flow1, flow2, flow3, flow4) { a, b, c, d ->
    intArrayOf(a, b, c, d)
}
val result = combine(numbersFlow, listFlow1, listFlow2) { nums, list1, list2 ->
    MyData(totalA = nums[0], totalB = nums[1], ...)
}
```

### Month-picker driven screens (flatMapLatest)
```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
val historyEntries = _historyMonth.flatMapLatest { anchor ->
    val from = DateUtils.startOfMonth(anchor)
    val to   = DateUtils.endOfMonth(anchor)
    repository.observeEntriesInRange(from, to)
}
```

### Atomic multi-step writes (withTransaction)
```kotlin
suspend fun addEntry(...) = withContext(Dispatchers.IO) {
    database.withTransaction {
        productionEntryDao.insert(entry)
        stockLedgerDao.insert(ledger)
        ensureAttendance(workerId, date)
    }
}
```

### Hilt ViewModel injection
Every ViewModel uses `@HiltViewModel` + `@Inject constructor`. Screens obtain it via `hiltViewModel()` from `androidx.hilt.navigation.compose`.

---

## Navigation routes

All routes are constants in `ui/navigation/Routes`. Path parameters use `/{param}`, optional params use `?param={param}` with `defaultValue`.

| Route constant | Screen | Notes |
|---|---|---|
| `login` | LoginScreen | start destination; pops itself on success |
| `dashboard` | DashboardScreen | bottom tab |
| `workers` | WorkerListScreen | bottom tab |
| `production` | ProductionScreen | bottom tab |
| `sales` | SalesScreen | bottom tab |
| `stock` | StockScreen | bottom tab |
| `add_worker?workerId={id}` | AddWorkerScreen | edit if id present |
| `worker_type_history/{id}` | WorkerTypeHistoryScreen | |
| `attendance` | AttendanceScreen | |
| `advance_entry?workerId={id}` | AdvanceEntryScreen | |
| `add_production` | AddProductionScreen | |
| `production_history` | ProductionHistoryScreen | |
| `create_sale` | CreateSaleScreen | |
| `pending_payments` | PendingPaymentsScreen | |
| `payment_entry/{saleId}` | PaymentEntryScreen | |
| `stock_ledger/{productId}` | StockLedgerScreen | |
| `stock_adjustment` | StockAdjustmentScreen | |
| `expense` | ExpenseScreen | |
| `add_expense` | AddExpenseScreen | |
| `reports` | ReportsScreen | hub |
| `salary_report` | SalaryReportScreen | |
| `profit_loss_report` | ProfitLossReportScreen | |
| `production_report` | ProductionReportScreen | |
| `sales_report` | SalesReportScreen | |

Bottom nav appears only on the 5 main tab routes (`BottomNavRoutes` set in `Navigation.kt`).

---

## DB seed data

Seeded once in `KulhadDatabase.SeedCallback.onCreate()`:

| Table | Records |
|---|---|
| `products` | 8 sizes: 60, 80, 100, 120, 150, 175, 200, 250 ml |
| `piece_rates` | ₹1.20/piece for each product |
| `expense_types` | Labor, Soil, Transport |
| `users` | `owner@kulhad.com` / `kulhad123` (SHA-256 hashed) |

---

## End-to-end smoke test

1. `./gradlew assembleDebug` → `BUILD SUCCESSFUL`.
2. App launches → Login screen (dark theme).
3. Login with `owner@kulhad.com` / `kulhad123` → Dashboard with bottom nav.
4. Add a PIECE worker → appears with PIECE badge.
5. Add production entry (80ml, qty=100, defective=5) → Stock 80ml=95, earnings=₹114.00, attendance auto-marked present.
6. Create sale (80ml×50 @ ₹3.00) → total ₹150, Stock 80ml=45, Unpaid badge.
7. Add partial payment ₹100 → status Partial, pending ₹50.
8. Add Soil expense ₹500 → Expense tab shows ₹500.
9. P&L report → Sales ₹150 − Labor ₹114 − Soil ₹500 = −₹464 (red).
10. Stock adjustment −10 LOSS on 80ml → Stock=35, Ledger shows LOSS row.
11. Change worker PIECE→SALARY @₹500/day → Type History shows both rows.
12. All charts render without crashes on sparse/empty data.
