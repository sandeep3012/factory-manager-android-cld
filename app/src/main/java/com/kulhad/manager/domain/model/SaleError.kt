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
