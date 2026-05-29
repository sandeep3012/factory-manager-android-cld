package com.kulhad.manager.data.util

import com.kulhad.manager.domain.model.AuditInfo

/**
 * Stateless helpers for building [AuditInfo] snapshots at write time.
 *
 * All timestamps use [System.currentTimeMillis] (actual wall-clock time) — never the
 * WorkingDateManager "working date".  Business entry dates and audit timestamps are
 * intentionally kept separate.
 *
 * Typical repository usage:
 *
 *   // On INSERT:
 *   val audit = AuditUtils.createAudit(userSessionManager.currentUser.value)
 *   entity.copy(auditCreatedBy = audit.createdBy, auditCreatedAt = audit.createdAt)
 *
 *   // On UPDATE (read existing row first to preserve creation fields):
 *   val audit = AuditUtils.updateAudit(existing.auditCreatedBy, existing.auditCreatedAt,
 *                                       userSessionManager.currentUser.value)
 *   entity.copy(auditCreatedBy = audit.createdBy, auditCreatedAt = audit.createdAt,
 *               auditUpdatedBy = audit.updatedBy, auditUpdatedAt = audit.updatedAt)
 */
object AuditUtils {

    /**
     * Build the initial audit record for a brand-new row.
     * [createdAt] is stamped with the actual wall-clock time of the write.
     */
    fun createAudit(currentUser: String): AuditInfo = AuditInfo(
        createdBy = currentUser.ifBlank { "System" },
        createdAt = System.currentTimeMillis(),
        updatedBy = null,
        updatedAt = null
    )

    /**
     * Build an updated audit record, preserving the original creation fields unchanged.
     * [updatedAt] is stamped with the actual wall-clock time of the update.
     */
    fun updateAudit(
        oldCreatedBy: String,
        oldCreatedAt: Long,
        currentUser: String
    ): AuditInfo = AuditInfo(
        createdBy = oldCreatedBy,
        createdAt = oldCreatedAt,
        updatedBy = currentUser.ifBlank { "System" },
        updatedAt = System.currentTimeMillis()
    )
}
