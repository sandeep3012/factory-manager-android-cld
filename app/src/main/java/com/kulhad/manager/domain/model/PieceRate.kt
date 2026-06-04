package com.kulhad.manager.domain.model

/**
 * Domain model for a single piece-rate history entry.
 *
 * The "current" rate for a product is always the entry with the highest [effectiveFrom].
 * No explicit "active" flag is needed — the DAOs always ORDER BY effective_from DESC.
 *
 * [audit] carries write-audit metadata. Rows created before v5 migration will have
 * [AuditInfo.createdAt] == 0L (the migrated-row sentinel rendered as "—" by [AuditInfoCard]).
 */
data class PieceRate(
    val id: Long,
    val productId: Long,
    val ratePerPiece: Double,
    val effectiveFrom: Long,
    val audit: AuditInfo
)
