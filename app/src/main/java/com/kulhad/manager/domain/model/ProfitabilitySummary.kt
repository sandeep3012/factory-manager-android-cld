package com.kulhad.manager.domain.model

/**
 * Reactive snapshot of the P&L breakdown for a given date range.
 *
 * All values are in whole rupees (Int).  [labourCost] is derived from
 * [production_entries.rate_snapshot] so it always reflects the rates that were
 * current when each production entry was saved — historically accurate even after
 * subsequent rate changes.
 *
 * Sources:
 *   [revenue]    = SUM(sales.total_amount)
 *   [labourCost] = SUM((qty_produced − defective_qty) × rate_snapshot)
 *   [expenses]   = SUM(expenses.amount)
 *   [grossProfit]= revenue − labourCost
 *   [netProfit]  = revenue − labourCost − expenses
 */
data class ProfitabilitySummary(
    val revenue:     Int,
    val labourCost:  Int,
    val expenses:    Int,
    val grossProfit: Int,
    val netProfit:   Int
) {
    companion object {
        /** Neutral zero-value used as the initial StateFlow emission. */
        val ZERO = ProfitabilitySummary(
            revenue     = 0,
            labourCost  = 0,
            expenses    = 0,
            grossProfit = 0,
            netProfit   = 0
        )
    }
}
