# Kulhad Manager

A production-ready Android app for managing a kulhad (clay cup) factory — tracking workers, production output, inventory, sales (including credit/udhaar), expenses, and profit reports.

Built for a 3-partner small manufacturing business. Runs fully offline on a local Room database with no internet dependency.

---

## Features

| Module | What it does |
|---|---|
| **Workers** | Register workers as Piece-rate or Daily-salary. Track attendance, type history, and cash advances. |
| **Production** | Log daily output per size and worker. Auto-marks attendance. Calculates piece earnings using a rate snapshot at the time of entry. |
| **Stock** | Ledger-based inventory — every production, sale, loss, and adjustment creates a ledger row. Current stock = `SUM(quantity_change)`. |
| **Sales** | Create customer orders with multiple product lines. Tracks Paid / Partial / Unpaid status with partial payment support (udhaar). |
| **Expenses** | Record expenses by category (Labor, Soil, Transport, custom). Monthly breakdown with donut chart. |
| **Reports** | Salary report, Profit & Loss, Production report, Sales report — all navigable by month. |

---

## Screenshots

> _Add screenshots here after first build._

---

## Tech Stack

| Layer | Library / Version |
|---|---|
| Language | Kotlin 2.0 |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Repository pattern |
| DI | Hilt 2.51.1 |
| Database | Room 2.6.1 |
| Navigation | Navigation Compose 2.7.7 |
| Async | Kotlin Coroutines 1.8.1 + StateFlow |
| Build | AGP 8.5, KSP 2.0.0-1.0.21, Gradle 8.7 |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 35 (Android 15) |

---

## Getting Started

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android device or emulator running API 26+

### Build & Run

```bash
# Clone the repository
git clone <repo-url>
cd KulhadManager

# Debug build
./gradlew assembleDebug

# Install directly on a connected device
./gradlew installDebug
```

Or open the `KulhadManager/` folder in Android Studio and press **Run**.

### Demo Login

| Field | Value |
|---|---|
| Email | `owner@kulhad.com` |
| Password | `kulhad123` |

The database is seeded automatically on first launch with 8 product sizes (60–250 ml), default piece rates (₹1.20/piece), and the demo user account.

---

## Project Structure

```
app/src/main/java/com/kulhad/manager/
├── data/
│   ├── local/
│   │   ├── dao/            ← 14 Room DAOs (Flow reads, suspend writes)
│   │   ├── entity/         ← 14 @Entity classes + enums
│   │   └── KulhadDatabase  ← Room DB with seed callback
│   ├── repository/         ← 6 repositories with atomic transactions
│   └── util/               ← DateUtils, Money, PasswordHasher, StockThresholds
├── di/
│   ├── DatabaseModule      ← @Provides DB + all DAOs + all repos
│   └── SessionModule       ← SessionManager singleton
├── domain/model/           ← Pure Kotlin data classes
├── ui/
│   ├── charts/             ← Canvas-based bar, line, donut charts
│   ├── components/         ← Shared composables (TopBar, StatCard, etc.)
│   ├── navigation/         ← Routes constants + BottomTabs
│   ├── screens/            ← 23 screens across 8 feature packages
│   └── theme/              ← Color, Type, Theme (dark-first)
├── KulhadApp               ← @HiltAndroidApp entry point
└── MainActivity            ← NavHost + Scaffold + BottomNavBar
```

---

## Database Schema (14 tables)

| Table | Purpose |
|---|---|
| `users` | Login credentials (SHA-256 hashed password) |
| `workers` | Worker profiles with cached current type/rate |
| `worker_type_history` | Immutable log of every type/rate change |
| `worker_advances` | Cash advance records per worker |
| `attendance` | Daily present/absent per worker |
| `products` | Product catalogue (size in ml) |
| `piece_rates` | Global piece rate per product |
| `production_entries` | Daily production log with rate snapshot |
| `stock_ledger` | **Single source of truth** for all stock movements |
| `sales` | Sale headers (customer, total, date) |
| `sale_items` | Line items per sale |
| `payments` | Payments against a sale (udhaar support) |
| `expense_types` | Configurable expense categories |
| `expenses` | Individual expense records |

### Key Design Decisions

**Stock ledger as single source of truth**
`current_stock = SUM(quantity_change)` per product. Never update a counter. Every production entry, sale, loss, or manual adjustment creates a new ledger row.

**Rate snapshot**
`production_entries.rate_snapshot` stores the piece rate at entry time. Historical earnings are immutable even when rates change later.

**Auto-attendance**
Adding a production entry automatically inserts an `is_present = true` attendance row for that worker+day inside the same DB transaction.

**Udhaar (credit) tracking**
`pending = sale.total_amount − SUM(payments.amount)`. Status badges: **Paid** (pending = 0) · **Partial** (0 < paid < total) · **Unpaid** (paid = 0).

---

## Navigation

The app has 5 main bottom-nav tabs plus 18 sub-screens:

```
Login
└── Dashboard ──── Workers ──── Production ──── Sales ──── Stock
                      │              │              │          │
                  Add/Edit       Add Entry      Create Sale  Ledger
                  Type History   History        Pending      Adjustment
                  Attendance                    Payment
                  Advance
                  
                  ─── Expense ─── Reports
                        │            │
                     Add Expense  Salary · P&L · Production · Sales
```

---

## Seed Data

Applied once on first database creation:

| Table | Seeded records |
|---|---|
| `products` | 8 sizes: 60, 80, 100, 120, 150, 175, 200, 250 ml |
| `piece_rates` | ₹1.20 per piece for every size |
| `expense_types` | Labor, Soil, Transport |
| `users` | `owner@kulhad.com` / `kulhad123` |

---

## Stock Thresholds

Used for colour-coding the Stock screen and Dashboard alert count:

| Level | Condition | Colour |
|---|---|---|
| Critical | qty < 100 | Red |
| Low | 100 ≤ qty < 500 | Amber |
| Healthy | qty ≥ 500 | Green |

---

## Contributing

1. Fork the repository.
2. Create a feature branch: `git checkout -b feature/my-feature`
3. Commit your changes: `git commit -m "Add my feature"`
4. Push to the branch: `git push origin feature/my-feature`
5. Open a Pull Request.

Please follow the existing MVVM pattern — screens should be thin composables, all business logic lives in ViewModels and repositories, and every multi-table write must use `database.withTransaction { … }`.

---

## License

This project is private software developed for internal factory use.

---

## Acknowledgements

Built with [Jetpack Compose](https://developer.android.com/jetpack/compose), [Hilt](https://dagger.dev/hilt/), [Room](https://developer.android.com/training/data-storage/room), and [Navigation Compose](https://developer.android.com/jetpack/compose/navigation).
