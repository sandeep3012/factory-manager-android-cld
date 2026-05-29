package com.kulhad.manager.domain.model

/**
 * Immutable snapshot of who created / last-updated a record and when.
 *
 * Maps to the four audit columns present on every audited entity:
 *   audit_created_by  TEXT NOT NULL DEFAULT 'System'
 *   audit_created_at  INTEGER NOT NULL DEFAULT 0
 *   audit_updated_by  TEXT (nullable)
 *   audit_updated_at  INTEGER (nullable)
 *
 * [updatedBy] and [updatedAt] are null until the record is first edited after creation.
 * Use [AuditUtils] to build instances rather than constructing directly.
 */
data class AuditInfo(
    val createdBy: String,
    val createdAt: Long,
    val updatedBy: String?,
    val updatedAt: Long?
)
