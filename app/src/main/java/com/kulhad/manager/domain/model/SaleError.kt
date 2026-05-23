package com.kulhad.manager.domain.model

/**
 * Thrown inside a Room withTransaction when a sale item requests more stock than is available.
 * The transaction rolls back automatically — no Sale, SaleItem, StockLedger, or Payment rows
 * are created when this is thrown.
 *
 * For edit-sale scenarios, [requested] should be the NET additional quantity needed
 * (newQty - previousQty), so validation only checks what extra stock is consumed.
 */
class InsufficientStockException(
    val productName: String,
    val available: Int,
    val requested: Int
) : Exception(
    "Insufficient stock for $productName. Available: $available, Requested: $requested"
)

/**
 * Thrown inside a Room withTransaction when a payment amount would push the total paid
 * above the sale total. The transaction rolls back automatically — no Payment row is created.
 *
 * [total]     — sale.totalAmount
 * [paid]      — sum of all existing payments for this sale before this attempt
 * [pending]   — total - paid (the maximum allowed payment)
 * [attempted] — the amount the user tried to save
 */
class OverpaymentException(
    val total: Int,
    val paid: Int,
    val pending: Int,
    val attempted: Int
) : Exception(
    "Payment of $attempted exceeds pending amount $pending (total=$total, paid=$paid)"
)
