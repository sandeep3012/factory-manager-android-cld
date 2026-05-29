package com.kulhad.manager.domain.model

/**
 * Enriched domain model for a single production entry.
 *
 * [workerName] and [productSize] are resolved at the repository layer via a `combine`
 * across the entry, worker, and product tables — avoiding repeated lookups in the UI.
 *
 * [audit] carries write-audit metadata from [com.kulhad.manager.data.local.entity.ProductionEntryEntity].
 * Production entries are write-once (no edit flow yet), so [AuditInfo.updatedBy] and
 * [AuditInfo.updatedAt] will always be null in practice. They are included for
 * architectural consistency and to support a future edit flow without schema changes.
 */
data class ProductionEntry(
    val id: Long,
    val workerId: Long,
    val workerName: String,
    val productId: Long,
    val productSize: Int,
    val quantityProduced: Int,
    val defectiveQuantity: Int,
    val rateSnapshot: Double,
    val date: Long,
    val createdBy: Long,
    val createdAt: Long,
    val audit: AuditInfo
) {
    val netQty: Int get() = quantityProduced - defectiveQuantity
    val earnings: Double get() = netQty * rateSnapshot
}
