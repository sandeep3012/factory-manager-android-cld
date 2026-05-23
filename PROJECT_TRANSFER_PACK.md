# Project Transfer Pack — Kulhad Manager

---

## 1. Project Overview

- **App name:** Kulhad Manager
- **Purpose:** Internal business management app for a clay kulhad (cup) manufacturing factory. Runs on a single Android device shared among partners — not a consumer app.
- **Main users:** 3 factory partners / owners who operate the app themselves.
- **Core problem solved:** Replace manual paper registers for daily factory operations — track workers, production output, inventory stock, sales with credit (udhaar), expenses, and generate automated profit/loss and salary reports.

---

## 2. Tech Stack

| Category | Choice |
|---|---|
| **Language** | Kotlin 2.0 |
| **UI Framework** | Jetpack Compose + Material 3 |
| **Architecture pattern** | MVVM (ViewModel + Repository + Room) |
| **DI** | Hilt 2.51.1 (Dagger) |
| **Database** | Room 2.6.1 (SQLite) |
| **Navigation** | Navigation Compose 2.7.7 |
| **State management** | StateFlow + collectAsStateWithLifecycle |
| **Async** | Kotlin Coroutines 1.8.1 |
| **Charts** | Custom Canvas-based (SimpleBarChart, SimpleLineChart, DonutChart, HorizontalBarChart) — Vico 1.15.0 is declared in Gradle but NOT used in charts |
| **Preferences** | DataStore Preferences 1.1.1 (session storage) |
| **Build system** | Gradle 8.7, AGP 8.5.0, Kotlin DSL (.kts) |
| **Min SDK** | 26 (Android 8.0) |
| **Compile/Target SDK** | 35 |
| **Payment gateways** | None |
| **APIs** | None — fully offline, no network |
| **Services** | None — no background services |

### Key library versions (from `gradle/libs.versions.toml`)
```
kotlin           = 2.0.0
agp              = 8.5.0
composeBom       = 2024.06.00
hilt             = 2.51.1
room             = 2.6.1
navigationCompose= 2.7.7
coroutines       = 1.8.1
ksp              = 2.0.0-1.0.21
lifecycle        = 2.8.3
```

---

## 3. Project Structure

### Folder structure
```
KulhadManager/
├── app/
│   └── src/main/java/com/kulhad/manager/
│       ├── data/
│       │   ├── local/
│       │   │   ├── dao/               ← 14 Room DAOs
│       │   │   ├── entity/            ← 14 @Entity classes + 2 enums
│       │   │   ├── Converters.kt
│       │   │   └── KulhadDatabase.kt  ← DB singleton + seed callback
│       │   ├── repository/            ← 6 repository classes
│       │   └── util/
│       │       ├── DateUtils.kt       ← epoch millis helpers
│       │       ├── Money.kt           ← Indian rupee formatting
│       │       ├── PasswordHasher.kt  ← SHA-256
│       │       └── StockThresholds.kt ← CRITICAL<100, LOW<500
│       ├── di/
│       │   ├── DatabaseModule.kt      ← @Provides for DB + all DAOs + repos
│       │   └── SessionModule.kt       ← SessionManager singleton
│       ├── domain/model/              ← Clean data classes for UI layer
│       ├── ui/
│       │   ├── charts/                ← All Canvas-based chart composables
│       │   ├── components/            ← Reusable UI components
│       │   ├── navigation/            ← Routes object + BottomTabs
│       │   ├── preview/               ← UiDemoData.kt (dev-only overlay)
│       │   ├── screens/               ← 23 screens in 8 packages
│       │   │   ├── auth/
│       │   │   ├── dashboard/
│       │   │   ├── workers/
│       │   │   ├── production/
│       │   │   ├── sales/
│       │   │   ├── stock/
│       │   │   ├── expense/
│       │   │   └── reports/
│       │   └── theme/
│       │       ├── Color.kt
│       │       ├── Type.kt
│       │       └── Theme.kt
│       ├── KulhadApp.kt               ← @HiltAndroidApp
│       └── MainActivity.kt            ← @AndroidEntryPoint, NavHost
├── keystore.properties                ← GITIGNORED — signing passwords
├── kulhad-manager-release.jks         ← GITIGNORED — release keystore
└── gradle/libs.versions.toml          ← Version catalog
```

### Main modules
- **data** — Room entities, DAOs, repositories, utility helpers
- **di** — Hilt dependency injection modules
- **domain** — Pure Kotlin data classes (no Android deps)
- **ui** — All composables, ViewModels, navigation

### Navigation flow
```
LoginScreen (start)
    └── Dashboard (bottom nav tab 1)
    └── Workers (bottom nav tab 2)
            ├── AddWorkerScreen (add / edit)
            ├── WorkerTypeHistoryScreen
            ├── AttendanceScreen
            └── AdvanceEntryScreen
    └── Production (bottom nav tab 3)
            ├── AddProductionScreen
            └── ProductionHistoryScreen
    └── Sales (bottom nav tab 4)
            ├── CreateSaleScreen
            ├── PendingPaymentsScreen
            └── PaymentEntryScreen
    └── Stock (bottom nav tab 5)
            ├── StockLedgerScreen
            └── StockAdjustmentScreen
    └── ExpenseScreen (from Dashboard quick action)
            └── AddExpenseScreen
    └── ReportsScreen (from Dashboard quick action)
            ├── SalaryReportScreen
            ├── ProfitLossReportScreen
            ├── ProductionReportScreen
            └── SalesReportScreen
```
Bottom navigation bar is **only visible** on the 5 main tab routes (Dashboard, Workers, Production, Sales, Stock). All sub-screens have a back arrow via `KulhadTopBar`.

### Important classes/services

| Class | Role |
|---|---|
| `KulhadDatabase` | Room DB singleton, version=1, seeds data on first create |
| `SessionManager` | DataStore-backed singleton storing `currentUserId` and `currentUserName` |
| `WorkerRepository` | `changeType()` = atomic transaction: update workers cache + insert history row |
| `ProductionRepository` | `addEntry()` = atomic transaction: insert entry + stock ledger + ensure attendance |
| `SaleRepository` | `createSale()` = atomic transaction: insert sale + items + one stock ledger row per item |
| `StockRepository` | `adjust()` = insert LOSS or ADJUSTMENT ledger row |
| `ReportRepository` | Period-bounded queries for all 4 report types |
| `UiDemoData` | Dev-only overlay — shows realistic fake data when DB is empty. Controlled by `SHOW_DEMO` flag |

---

## 4. Screens and Features

### Auth
**LoginScreen**
- Purpose: Email + password auth gate
- Inputs: email, password
- Outputs: navigates to Dashboard on success, pops LoginScreen from back-stack
- Business logic: SHA-256 hash of entered password compared to `users.password_hash`
- Dependencies: `AuthViewModel`, `UserDao`, `SessionManager`

---

### Dashboard
**DashboardScreen**
- Purpose: Factory overview — KPIs, chart, quick actions, shortcuts
- Inputs: none (reactive from DB)
- Outputs: navigation to all sub-sections via Quick Actions and Shortcuts
- Business logic: Combines 5+ flows (revenue, production 7-day, workers present/total, stock alerts, sales today). Demo overlay when DB is empty.
- Dependencies: `DashboardViewModel`, all repositories

---

### Workers module (5 screens)

**WorkerListScreen**
- Purpose: List all workers with present/absent status badges, KPI strip, filter by ALL/PIECE/SALARY
- Inputs: filter selection
- Outputs: navigate to AddWorker, TypeHistory, Attendance, Advance
- Dependencies: `WorkerViewModel`

**AddWorkerScreen** (doubles as Edit)
- Purpose: Add new worker or edit existing (pre-fills if `workerId` arg present)
- Inputs: name, phone, address, join date, type (PIECE/SALARY), daily rate (salary only)
- Outputs: inserts or updates worker + first `worker_type_history` row
- Business logic: If PIECE type, `daily_rate = 0`. Rate and type always saved to history on add.
- Dependencies: `WorkerViewModel`

**WorkerTypeHistoryScreen**
- Purpose: View full type/rate change history for one worker, trigger type change
- Inputs: `workerId` from nav arg
- Outputs: new `worker_type_history` row + updates `workers` cache row
- Business logic: Effective from = today's start-of-day. `changeType()` is one transaction.
- Dependencies: `WorkerViewModel`

**AttendanceScreen**
- Purpose: Mark present/absent for all workers for a selected date
- Inputs: date picker, toggle per worker
- Outputs: upserts `attendance` rows
- Dependencies: `WorkerViewModel`

**AdvanceEntryScreen**
- Purpose: Record a salary advance paid to a worker
- Inputs: worker picker, amount, date, optional remark
- Outputs: inserts `worker_advances` row; lists past advances below
- Dependencies: `WorkerViewModel`

---

### Production module (3 screens)

**ProductionScreen**
- Purpose: 7-day production KPI + bar chart, piece rate table
- Inputs: none (reactive)
- Outputs: navigate to AddProduction, ProductionHistory
- Dependencies: `ProductionViewModel`

**AddProductionScreen**
- Purpose: Log daily production for a worker
- Inputs: worker, size (product), quantity produced, defective count, date
- Outputs: inserts `production_entries` + `stock_ledger (PRODUCTION, +qty)` + auto-marks attendance
- Business logic: net pieces = qty − defective; earnings = net × `rate_snapshot` (rate looked up from `piece_rates` at save time and frozen)
- Dependencies: `ProductionViewModel`

**ProductionHistoryScreen**
- Purpose: Browse past production entries with month navigation
- Inputs: month picker (prev/next arrows)
- Outputs: read-only list
- Dependencies: `ProductionViewModel` (`flatMapLatest` on month state)

---

### Sales module (4 screens)

**SalesScreen**
- Purpose: Weekly sales KPI, line chart, recent sales list with payment status badges
- Inputs: none (reactive)
- Outputs: navigate to CreateSale, PendingPayments
- Dependencies: `SalesViewModel`

**CreateSaleScreen**
- Purpose: Create a new sale with multiple line items
- Inputs: customer name, product size pills, quantity, price per unit (per item)
- Outputs: inserts `sales` + `sale_items` + one `stock_ledger (SALE, −qty)` per item
- Business logic: Each item reduces stock. Sale starts as Unpaid (no payments yet).
- Bottom sheet: "Add item" opens `AddItemSheet` (LazyColumn, `windowInsets = WindowInsets.ime`)
- Dependencies: `SalesViewModel`

**PendingPaymentsScreen**
- Purpose: List all sales where pending > 0
- Inputs: none (reactive)
- Outputs: navigate to PaymentEntry per sale
- Business logic: `pending = total_amount − SUM(payments.amount)`
- Dependencies: `SalesViewModel`

**PaymentEntryScreen**
- Purpose: Record a partial or full payment against a sale
- Inputs: `saleId` nav arg, amount, date, optional remark
- Outputs: inserts `payments` row; sale status auto-updates (Partial → Paid when pending = 0)
- Dependencies: `SalesViewModel`

---

### Stock module (3 screens)

**StockScreen**
- Purpose: Current stock per size with colour-coded health, KPI strip, bar chart
- Inputs: none (reactive)
- Outputs: navigate to StockLedger (per product), StockAdjustment
- Business logic: Stock = `SUM(quantity_change)` in `stock_ledger` per product. CRITICAL < 100, LOW < 500, HEALTHY ≥ 500.
- Dependencies: `StockViewModel`

**StockLedgerScreen**
- Purpose: Full change history for one product size
- Inputs: `productId` nav arg
- Outputs: read-only chronological list of all stock events
- Dependencies: `StockViewModel`

**StockAdjustmentScreen**
- Purpose: Manual stock correction (loss, write-off)
- Inputs: product (size pill), quantity, type (LOSS / ADJUSTMENT), remark
- Outputs: inserts `stock_ledger` row with negative quantity
- Dependencies: `StockViewModel`

---

### Expense module (2 screens)

**ExpenseScreen**
- Purpose: Total expense KPI, donut breakdown chart by category, recent list
- Inputs: none (reactive)
- Outputs: navigate to AddExpense
- Dependencies: `ExpenseViewModel`

**AddExpenseScreen**
- Purpose: Log a factory expense
- Inputs: expense type (Labor / Soil / Transport), amount, date, remark
- Outputs: inserts `expenses` row
- Dependencies: `ExpenseViewModel`

---

### Reports module (5 screens)

**ReportsScreen**
- Purpose: Hub with 4 report cards
- Outputs: navigate to SalaryReport, ProfitLossReport, ProductionReport, SalesReport

**SalaryReportScreen**
- Purpose: Monthly salary computation for all workers
- Inputs: month picker
- Business logic: PIECE workers = net_pieces × rate_snapshot summed from production_entries. SALARY workers = attendance days × daily_rate from history. Advances summed and shown separately.
- Dependencies: `ReportsViewModel`

**ProfitLossReportScreen**
- Purpose: Monthly P&L with trend chart and category breakdown
- Inputs: month picker
- Business logic: Net = total_sales − (labor + soil + transport + other expenses)
- Dependencies: `ReportsViewModel`

**ProductionReportScreen**
- Purpose: Monthly production stats — KPI strip, bar chart by size, top workers
- Inputs: month picker
- Dependencies: `ReportsViewModel`

**SalesReportScreen**
- Purpose: Monthly sales — KPI strip, 7-day line chart, top customers, collected/pending donut
- Inputs: month picker
- Dependencies: `ReportsViewModel`

---

## 5. Completed Work

### Fully implemented
- ✅ All 14 Room entities and DAOs
- ✅ All 6 repositories with atomic transactions
- ✅ Hilt DI module (DB + DAOs + Repos + SessionManager)
- ✅ All 23 screens with ViewModels
- ✅ Full navigation graph (MainActivity → NavHost, 23 routes)
- ✅ Bottom navigation bar (5 tabs, hides on sub-screens)
- ✅ Dark-first theme (Color.kt, Type.kt, Theme.kt — Material 3)
- ✅ All reusable UI components (KulhadTopBar, KulhadButton, KulhadTextField, KulhadTopBar, KpiStrip, StatusBadge, WorkerAvatar, SizePillGrid, SegmentedControl, BottomNavBar, StatCard, HeroCard, SectionHeader, ReportRow, EmptyState, LoadingState)
- ✅ All Canvas-based charts (SimpleBarChart, SimpleLineChart, DonutChart, MultiSegmentDonut, HorizontalBarChart)
- ✅ Demo data overlay system (UiDemoData) — controlled by `SHOW_DEMO` flag
- ✅ Dashboard redesigned with dark navy gradient, metric cards with icon badges, quick actions with hover effects
- ✅ UI scaled 20% globally (text sizes, icon sizes, corner radii, button heights)
- ✅ Bottom sheet inset fix — ModalBottomSheet uses `windowInsets = WindowInsets.ime`; content uses `LazyColumn` + `bottomSheetPadding()` modifier
- ✅ Release signing configured (keystore.properties → signingConfigs → buildTypes.release)
- ✅ DB seed data (8 products, 8 piece rates at ₹1.20, 3 expense types, demo user)

### Important implementation details
- **Stock** is never stored as a column — always `SUM(quantity_change)` query at runtime
- **Rate snapshot**: piece rate frozen at entry time in `production_entries.rate_snapshot`; changing rates never affects old history
- **Auto-attendance**: Adding a production entry auto-creates an attendance row in the same DB transaction
- **Worker type cache**: `workers.current_type` + `workers.daily_rate` are a cache of latest `worker_type_history` row, kept in sync by `WorkerRepository.changeType()`
- **Bottom sheet fix**: All bottom sheets set `windowInsets = WindowInsets.ime` on `ModalBottomSheet` and use `LazyColumn` + reusable `bottomSheetPadding()` modifier (`imePadding()` + `navigationBarsPadding()`) in `BottomSheetUtils.kt`
- **Demo data**: All usage guarded by `UiDemoData.SHOW_DEMO && <real data empty>`. Safe to set `SHOW_DEMO = true` — demo disappears automatically when real data exists

---

## 6. Current Work In Progress

- Nothing actively in progress.
- **`SHOW_DEMO` is currently set to `true`** in `UiDemoData.kt` (user set this; demo data is safely hidden once real data is inserted).
- Release keystore has been generated and signing is configured; app is ready for signed APK generation.

### Current blockers
- None

---

## 7. Known Bugs and Attempted Fixes

### Bug 1 — "Add to Sale" button hidden behind navigation bar
- **Issue:** Bottom sheet in `CreateSaleScreen` extended behind Android system navigation bar. "Add to Sale" button was not clickable without scrolling.
- **Root cause:** `ModalBottomSheet` by default consumes all system insets including navigation bar. Child content's `navigationBarsPadding()` had no effect.
- **Solutions tried:**
  1. Applied `navigationBarsPadding()` to content Column — button still hidden initially
  2. Switched Column to LazyColumn for scrollability — button accessible after scrolling but still hidden on open
  3. **Final fix:** Set `windowInsets = WindowInsets.ime` on `ModalBottomSheet` itself + LazyColumn with `bottomSheetPadding()` modifier
- **Current status:** ✅ RESOLVED — button always visible from first render on all navigation modes

### Bug 2 — Same inset issue in WorkerTypeHistoryScreen
- **Issue:** "Save change" button in `ChangeTypeSheet` had the same problem
- **Root cause:** Same as Bug 1
- **Fix:** Same fix applied — `windowInsets = WindowInsets.ime` + LazyColumn + `bottomSheetPadding()`
- **Current status:** ✅ RESOLVED

---

## 8. Coding Rules and Conventions

### Naming rules
- Screens: `<Name>Screen.kt` — `@Composable fun <Name>Screen(...)`
- ViewModels: `<Module>ViewModel.kt` — e.g., `WorkerViewModel` serves all 5 worker screens
- Repositories: `<Entity>Repository.kt`
- DAOs: `<Entity>Dao.kt`
- Entities: `<Entity>Entity.kt`
- Domain models: Plain class name, e.g., `Worker`, `Sale`, `Stock`
- Routes: All-caps constant in `Routes` object, e.g., `Routes.WORKERS`
- Colors: Semantic names — `BgDeep`, `SurfaceCard`, `TextPrimary`, `Success`, `ErrorRed`

### UI rules
- All screens use `KulhadTopBar` for the top bar (title + optional subtitle + optional actions)
- All primary action buttons use `KulhadButton` (not raw Material Button)
- All text fields use `KulhadTextField` (custom styled wrapper)
- All status chips use `StatusBadge(text, BadgeType.SUCCESS/ERROR/INFO/WARNING)`
- Every bottom sheet must use `windowInsets = WindowInsets.ime` on `ModalBottomSheet` and `LazyColumn` + `.bottomSheetPadding()` on content
- Empty states use `EmptyState` composable; loading states use `LoadingState`
- UI scale factor is 1.2× (20% larger than default Material 3) — do not revert to default sizes
- All screens have `modifier = Modifier.fillMaxSize().background(BgDeep)`

### Architecture decisions
- MVVM strictly — screens only call ViewModel functions, never repositories directly
- ViewModels expose `StateFlow` or `Flow` only — no `LiveData`
- All DB reads return `Flow<T>` (reactive); all DB writes are `suspend` functions
- Repositories own all multi-step transactions using `database.withTransaction { }`
- Use `collectAsStateWithLifecycle()` (not `collectAsState()`) in all Composables
- Use `@OptIn(ExperimentalCoroutinesApi::class) flatMapLatest` for month-picker-driven flows

### Reusable patterns

**Pattern 1: combine() with > 4 typed flows**
```kotlin
val nums = combine(flow1, flow2, flow3) { a, b, c -> intArrayOf(a, b, c) }
val result = combine(nums, listFlow) { n, list -> MyData(n[0], n[1], n[2], list) }
```

**Pattern 2: Month-picker driven query**
```kotlin
val _month = MutableStateFlow(System.currentTimeMillis())
val data = _month.flatMapLatest { anchor ->
    repository.observeInRange(DateUtils.startOfMonth(anchor), DateUtils.endOfMonth(anchor))
}
```

**Pattern 3: Atomic multi-step write**
```kotlin
suspend fun saveEntry(...) = withContext(Dispatchers.IO) {
    database.withTransaction {
        dao1.insert(...)
        dao2.insert(...)
    }
}
```

**Pattern 4: Bottom sheet with proper insets**
```kotlin
ModalBottomSheet(
    windowInsets = WindowInsets.ime,   // KEY — let content handle nav bar
    containerColor = SurfaceCard,
    ...
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth().bottomSheetPadding(),
        contentPadding = PaddingValues(16.dp)
    ) { /* items */ }
}
```

**Pattern 5: Demo data overlay**
```kotlin
val useDemo = UiDemoData.SHOW_DEMO && realData.isEmpty()
val displayValue = if (useDemo) UiDemoData.someValue else realData.value
```

### Restrictions
- **Never** update a stock quantity counter column directly — always insert a `stock_ledger` row
- **Never** use hardcoded bottom padding for navigation bar — always use `navigationBarsPadding()`
- **Never** use `LiveData` — use `StateFlow`/`Flow` only
- **Never** call repositories directly from Composables — always go through ViewModel
- **Never** use Vico chart library for rendering — all charts are custom Canvas composables (Vico is in Gradle but unused in charts)
- **Never** commit `keystore.properties` or `*.jks` to git
- **Never** use `collectAsState()` — always use `collectAsStateWithLifecycle()`

---

## 9. UI/UX Decisions

### Color system (dark-first, no light theme in use)
```
Background deep:    #0F172A  (BgDeep)
Background login:   #0A0F1E  (BgLogin)
Nav surface:        #111827  (SurfaceNav)
Card surface:       #1E293B  (SurfaceCard)
Border:             #334155  (BorderLine)
Text primary:       #F1F5F9  (TextPrimary)
Text secondary:     #94A3B8  (TextSecondary)
Text tertiary:      #64748B  (TextTertiary)
Brand blue:         #3B82F6  (PrimaryBlue)
Brand blue dark:    #1D4ED8  (PrimaryBlueDark)
Success green:      #34D399  (Success)
Error red:          #F87171  (ErrorRed)
Warning amber:      #FBBF24  (WarningAmber)
Info blue:          #60A5FA  (InfoBlue)
Purple accent:      #A78BFA  (PurpleAccent)
```

### Typography scale (20% scaled up from defaults)
```
Display:   24sp (was 20sp)
Title:     18sp (was 15sp)
Body:      17sp (was 14sp)
Label:     13sp (was 11sp)
Caption:   11sp (was 9sp)
```

### Animation preferences
- `animateColorAsState` used for hover effects on Dashboard quick-action buttons
- No page transition animations (default Compose Navigation transitions)
- No explicit spring/tween animations elsewhere — keep it simple

### Widget style
- Corner radius: 12dp (cards), 10dp (chips/badges), 14dp (buttons)
- Button height: implicit via vertical padding 14dp top/bottom
- All cards use `SurfaceCard` background with no visible border (border via subtle contrast)
- Status badges: small pill, semantic background/foreground colour pairs
- Charts: flat, no legends, labels below bars or at data points
- Dashboard: dark navy gradient (`Brush.verticalGradient(0xFF0F1419 → 0xFF1A1F2E)`) with boxy metric cards with icon badges (circular, 15% opacity coloured backgrounds)

### Design rules
- Dark theme only — no light mode support
- Minimum touch target: 48dp (enforced via button heights and icon button sizes)
- Section labels in ALL CAPS, 11sp, `TextSecondary`, letter-spacing 0.5sp
- Horizontal padding: 14dp on all list screens
- Vertical spacing between cards: 10dp

---

## 10. Prompt Patterns

### Fix issue prompt
```
In [FileName.kt], the [component/function] has this issue: [describe problem].
Root cause: [if known].
Fix it without changing any other behaviour or file. Keep existing imports and modifier order.
```

### Feature generation prompt
```
Add [feature name] to [ScreenName.kt].
The screen already has [existing structure].
The new feature should:
1. [behaviour 1]
2. [behaviour 2]
Use the existing ViewModel at [ViewModelName]. If new DAO/repository methods are needed, add them first.
Follow the project pattern: MVVM, StateFlow, collectAsStateWithLifecycle, KulhadButton, KulhadTextField.
```

### New screen prompt
```
Create [ScreenName].kt in ui/screens/[module]/.
Route: [route constant from Routes object]
Navigation args: [list args]
Purpose: [what it does]
Data: reads from [repository/DAO], writes to [repository/DAO]
UI: use KulhadTopBar with back arrow, LazyColumn, KulhadButton for primary action.
Follow the project's MVVM pattern. Add ViewModel function in [ViewModelName].kt.
```

### Refactor prompt
```
Refactor [FileName.kt] to [goal — e.g., extract X into a reusable composable].
Do not change any business logic or navigation behaviour.
Keep all existing imports. Place the new composable in ui/components/ if reusable across screens.
```

### UI scaling prompt
```
Scale all UI dimensions in [FileName.kt] by 1.2×.
Target: text sizes, icon sizes, corner radii, button heights, avatar sizes.
Do NOT change spacing/padding values unless they look broken.
Reference: body text should be 17sp, labels 13sp, captions 11sp.
```

### Bottom sheet prompt
```
Add a bottom sheet to [ScreenName.kt] triggered by [button/action].
Content: [list of inputs and a confirm button].
Use the project's bottom sheet pattern:
- ModalBottomSheet with windowInsets = WindowInsets.ime
- LazyColumn with Modifier.fillMaxWidth().bottomSheetPadding()
- contentPadding = PaddingValues(16.dp)
- KulhadButton as the last item
```

---

## 11. Roadmap

### Immediate next tasks
- Generate and test signed APK (`./gradlew assembleRelease`)
- Install on factory owner's device and run smoke test (see CLAUDE.md for 12-step checklist)

### Mid-term tasks
- Add worker deletion (soft delete via `is_active = false`) with confirmation dialog
- Add sale deletion / voiding support
- Add expense category management (add new expense types via UI instead of only seeded ones)
- Add date filter to sales and expense list screens (currently shows all or current month)
- Add search/filter by customer name on Sales screen
- Add PDF/share export for salary report

### Long-term tasks
- Multi-user support (currently single user; login exists but only one owner account)
- Play Store release (requires App Bundle, privacy policy, store listing)
- Backup / restore database to Google Drive
- Stock alerts as push notifications (requires background service)
- Piece rate management screen (currently seeded at ₹1.20 — no UI to change it)

---

## 12. Important Decisions Log

| Decision | Reason |
|---|---|
| **Stock as ledger (append-only), never a counter column** | Prevents race conditions; every stock change is auditable; enables StockLedger history screen |
| **Rate snapshot stored at production entry time** | Ensures historical earnings never change when piece rates are updated |
| **workers.current_type/daily_rate = cache of history** | Enables fast list queries while preserving full history for reports |
| **Auto-attendance on production entry** | Workers who produce must be present — saves manual double entry |
| **SHA-256 for password hashing** | Spec requirement; acknowledged as not production-grade (bcrypt would be better) |
| **All dates stored as start-of-day epoch millis** | Simplifies date range queries; avoids time-zone edge cases in SQL |
| **Indian number formatting (en_IN locale)** | Target market is India; 1,23,456 format is familiar to users |
| **Canvas-based charts instead of Vico** | Simpler API, full control over styling, no external API changes; Vico declared in Gradle but intentionally not used for charts |
| **Single ViewModel per module, not per screen** | Workers module has 5 screens sharing `WorkerViewModel`; reduces boilerplate, data is naturally shared |
| **Dark theme only, no light mode** | Factory environment often dim; owners preferred dark; avoids maintaining two theme variants |
| **UI scaled 20% globally** | Factory owners (older demographic) needed larger text and touch targets |
| **ModalBottomSheet windowInsets = WindowInsets.ime** | Default inset handling prevented child navigationBarsPadding() from working; this delegates nav bar to content |
| **LazyColumn in bottom sheets** | Prevents action buttons being hidden behind navigation bar; enables content to scroll when needed |
| **UiDemoData SHOW_DEMO flag** | Allows realistic UI visualization on empty DB; safely auto-hides when real data exists (AND logic) |
| **keystore.properties at project root, gitignored** | Standard Android signing practice; avoids hardcoded secrets in Gradle |
| **Package name: com.kulhad.manager** | Simple, identifies product and function; matches applicationId throughout |
| **Minimum SDK 26** | Covers Android 8.0+, which captures >97% of active devices; no legacy support needed |
| **foreignKey onDelete = SET_DEFAULT for user references** | User deletion is not a feature; SET_DEFAULT to 0 keeps records intact if user is ever removed |

---

## 13. Context Summary

**Kulhad Manager** is a fully offline Android app (Kotlin 2.0 + Jetpack Compose + Material 3) for a 3-partner clay kulhad (cup) factory in India. It has **no backend, no network calls, no payment gateways** — everything runs locally via Room SQLite.

**Architecture:** MVVM. 14 Room tables → 14 DAOs → 6 Repositories → ViewModels → 23 Compose screens. Hilt for DI. StateFlow + collectAsStateWithLifecycle for reactive UI. Navigation Compose for routing.

**Core business rules to never violate:**
1. Stock is always `SUM(stock_ledger.quantity_change)` per product — never a stored counter
2. Production entry = atomic transaction: entry + stock ledger row + attendance row
3. Sale creation = atomic transaction: sale + sale items + one stock deduction per item
4. Piece rate frozen at entry time (`rate_snapshot`) — changing rates does not affect history
5. Worker type changes = atomic: update workers cache + insert history row

**UI conventions:** Dark-first, all text 20% larger than Material defaults (body=17sp, labels=13sp), `KulhadButton` / `KulhadTextField` always used over raw Material components. All bottom sheets must use `windowInsets = WindowInsets.ime` + `LazyColumn` + `.bottomSheetPadding()`.

**Current state:** All 23 screens fully implemented and wired. Release signing configured. Demo data overlay exists (`UiDemoData.SHOW_DEMO = true`) but auto-hides when real data is present. App is ready for first install and smoke testing.

**Build command:** `./gradlew assembleRelease` from `D:\Sandeep\Kulhad Factory\KulhadManager\`. Requires `keystore.properties` with real passwords at project root.
