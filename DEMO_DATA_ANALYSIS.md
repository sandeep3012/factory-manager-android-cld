# Demo Data Auto-Hide Analysis

## The Question
**If I set `const val SHOW_DEMO = true` in UiDemoData.kt, will the dummy data automatically hide when real data is inserted?**

## Answer: ✅ YES, IT WILL AUTOMATICALLY HIDE

The dummy data uses **AND logic** (not OR logic), which means:
- Demo data shows **ONLY when BOTH conditions are true:**
  1. `SHOW_DEMO = true` 
  2. **AND** Real database is empty/zero

When you insert real data, condition #2 becomes **false**, so demo data automatically hides, regardless of SHOW_DEMO value.

---

## How Each Screen Controls Demo Data

### 1. **DashboardScreen** ✅
```kotlin
val isEmpty = UiDemoData.SHOW_DEMO && 
    raw.totalRevenueMonth == 0 &&              // CONDITION 1
    raw.production7Days.all { it == 0 }         // CONDITION 2

val net = if (isEmpty) UiDemoData.dashNetProfit else raw.netProfitMonth.toLong()
```

**Logic:** Demo shows only when SHOW_DEMO is true AND total revenue is 0 AND production is 0

---

### 2. **WorkerListScreen** ✅
```kotlin
val useDemo = UiDemoData.SHOW_DEMO && data.workers.isEmpty()
                                      // CONDITION: workers list is empty
val dispTotal = if (useDemo) UiDemoData.workerTotal else data.totalCount
```

**Logic:** Demo shows only when SHOW_DEMO is true AND workers table is empty

---

### 3. **ProductionScreen** ✅
```kotlin
val useDemo = UiDemoData.SHOW_DEMO && 
    stats.totalPieces == 0 &&                   // CONDITION 1
    stats.daily.all { it == 0 }                 // CONDITION 2

val dispTotal = if (useDemo) UiDemoData.productionTotal7d else stats.totalPieces
```

**Logic:** Demo shows only when SHOW_DEMO is true AND total pieces is 0 AND daily stats are all 0

---

### 4. **SalesScreen** ✅
```kotlin
val useDemo = UiDemoData.SHOW_DEMO && 
    data.weekTotal == 0 &&                      // CONDITION 1
    data.recent.isEmpty()                       // CONDITION 2

val dispWeekTotal = if (useDemo) UiDemoData.salesWeekTotal else data.weekTotal.toLong()
```

**Logic:** Demo shows only when SHOW_DEMO is true AND week total is 0 AND recent sales list is empty

---

### 5. **StockScreen** ✅
```kotlin
val useDemo = UiDemoData.SHOW_DEMO && items.all { it.quantity == 0 }
                                      // CONDITION: all stock quantities are 0

val displayItems = if (useDemo) { /* demo */ } else { /* real */ }
```

**Logic:** Demo shows only when SHOW_DEMO is true AND all stock items have quantity 0

---

### 6. **Report Screens** (Salary, Profit/Loss, Production, Sales) ✅
```kotlin
// SalaryReportScreen
val useDemo = UiDemoData.SHOW_DEMO && report == null

// SalesReportScreen
val useDemo = UiDemoData.SHOW_DEMO && report == null

// ProfitLossReportScreen
val useDemo = UiDemoData.SHOW_DEMO && report == null
```

**Logic:** Demo shows only when SHOW_DEMO is true AND report data is null (hasn't been generated)

---

## Real-World Scenario

### Scenario 1: SHOW_DEMO = false (Current State)
```
SHOW_DEMO = false
├─ Demo data always hidden ✅
└─ Only real data shown (even if database is empty → shows empty state)
```

### Scenario 2: SHOW_DEMO = true, Database Empty
```
SHOW_DEMO = true
├─ Real data is empty/zero
└─ Demo data shown ✅ (for visualization during development)
```

### Scenario 3: SHOW_DEMO = true, After Inserting Real Data
```
SHOW_DEMO = true
├─ Real data exists (not empty/zero anymore)
└─ Demo data AUTOMATICALLY HIDDEN ✅
    ↳ Real data shown instead
```

---

## Key Insight: The AND Logic

Every screen follows the same pattern:

```
Display Demo Data = SHOW_DEMO && (Real Data is Empty)
                    ├─ true    ├─ true  → Show Demo ✅
                    ├─ true    └─ false → Show Real Data ✅
                    └─ false            → Show Real Data (or Empty State) ✅
```

The **&&** operator ensures that even if SHOW_DEMO = true, the moment you insert real data:
- The `(Real Data is Empty)` condition becomes **false**
- The entire expression becomes **false**  
- Real data is displayed instead

---

## What Gets Checked as "Empty"

| Screen | Empty Condition |
|--------|-----------------|
| Dashboard | `totalRevenueMonth == 0` AND `production7Days.all { it == 0 }` |
| Workers | `workers.isEmpty()` |
| Production | `totalPieces == 0` AND `daily.all { it == 0 }` |
| Sales | `weekTotal == 0` AND `recent.isEmpty()` |
| Stock | `items.all { it.quantity == 0 }` |
| Reports | `report == null` |

---

## Conclusion

✅ **YES, demo data will automatically hide when real data is inserted**

- The logic is `SHOW_DEMO && RealDataIsEmpty`
- Not `SHOW_DEMO && RealDataExists`
- When real data exists, the second condition is false
- Therefore, real data is always shown (demo data hidden)

**This is safe and intentional design.** You can set SHOW_DEMO = true for development/testing without worry that old demo data will linger in production.
