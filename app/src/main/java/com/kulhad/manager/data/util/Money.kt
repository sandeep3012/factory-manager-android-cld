package com.kulhad.manager.data.util

import java.text.NumberFormat
import java.util.Locale

object Money {

    private val inrFormat: NumberFormat by lazy {
        NumberFormat.getInstance(Locale("en", "IN"))
    }

    /** Format a whole-rupee amount as "₹1,23,456". */
    fun formatRupees(amount: Int): String = "₹${inrFormat.format(amount)}"

    fun formatRupees(amount: Long): String = "₹${inrFormat.format(amount)}"

    /** Format a Double with up to 2 decimals, Indian grouping. */
    fun formatRupeesDouble(amount: Double): String {
        val fmt = NumberFormat.getInstance(Locale("en", "IN"))
        fmt.maximumFractionDigits = 2
        fmt.minimumFractionDigits = if (amount % 1.0 == 0.0) 0 else 2
        return "₹${fmt.format(amount)}"
    }
}
