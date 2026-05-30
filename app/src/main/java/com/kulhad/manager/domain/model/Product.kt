package com.kulhad.manager.domain.model

/**
 * Domain model for a kulhad product/size.
 *
 * [displayLabel] is the human-readable label shown in pickers (e.g. "80ml", "Half Litre").
 * Falls back to "${sizeMl}ml" in the repository when the stored value is blank (migrated rows).
 *
 * [displayOrder] controls the sort order in all product pickers. Lower value = appears first.
 *
 * [audit] carries write-audit metadata. Products are editable (name, label, order, status)
 * so [AuditInfo.updatedBy] / [AuditInfo.updatedAt] will be non-null after the first edit.
 */
data class Product(
    val id: Long,
    val sizeMl: Int,
    val description: String,
    val isActive: Boolean,
    val displayLabel: String,
    val displayOrder: Int,
    val audit: AuditInfo      // same package — no import needed
)

data class ProductWithRate(
    val product: Product,
    val ratePerPiece: Double,
    val effectiveFrom: Long
)
