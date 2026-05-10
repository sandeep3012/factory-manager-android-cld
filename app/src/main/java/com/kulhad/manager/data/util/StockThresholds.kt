package com.kulhad.manager.data.util

object StockThresholds {
    const val CRITICAL = 100
    const val LOW = 500
    // HEALTHY = >= LOW

    enum class Level { CRITICAL, LOW, HEALTHY }

    fun classify(qty: Int): Level = when {
        qty < CRITICAL -> Level.CRITICAL
        qty < LOW -> Level.LOW
        else -> Level.HEALTHY
    }

    fun isAlert(qty: Int): Boolean = qty < LOW
}
