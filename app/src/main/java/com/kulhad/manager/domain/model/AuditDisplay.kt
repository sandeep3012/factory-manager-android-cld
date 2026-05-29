package com.kulhad.manager.domain.model

/**
 * Pure UI/domain model for rendering audit information on detail and history screens.
 *
 * Distinct from [AuditInfo] (which is used by the data layer for write operations and
 * carries direct column semantics). [AuditDisplay] is the read-only, presentation-layer
 * view of the same data — consumed by composables via [AuditInfoCard].
 *
 * Obtain via [com.kulhad.manager.data.util.toDisplay] extension on [AuditInfo]:
 *   val display = auditInfo.toDisplay()
 */
data class AuditDisplay(
    val createdBy: String,
    val createdAt: Long,
    val updatedBy: String?,
    val updatedAt: Long?
)
