package com.kulhad.manager.domain.model

import com.kulhad.manager.data.local.entity.StockChangeType

// AuditInfo is in the same package — no import needed.

data class StockItem(
    val product: Product,
    val quantity: Int
)

/**
 * Domain model for a single stock-ledger row, enriched with product/user display info.
 *
 * [audit] carries write-audit metadata from [com.kulhad.manager.data.local.entity.StockLedgerEntity].
 * For LOSS and ADJUSTMENT rows an edit flow exists; [AuditInfo.updatedBy] / [AuditInfo.updatedAt]
 * will be non-null after the first edit. PRODUCTION and SALE rows are write-once; their
 * updated fields will always be null.
 */
data class StockMovement(
    val id: Long,
    val productId: Long,
    val productSize: Int,
    val quantityChange: Int,
    val type: StockChangeType,
    val remark: String,
    val description: String,
    val doneBy: Long,
    val doneByName: String,
    val timestamp: Long,
    val audit: AuditInfo
)
