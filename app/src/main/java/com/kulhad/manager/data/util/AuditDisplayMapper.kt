package com.kulhad.manager.data.util

import com.kulhad.manager.domain.model.AuditDisplay
import com.kulhad.manager.domain.model.AuditInfo

/**
 * Bridges the data-layer [AuditInfo] (used for write operations and repository mapping) and
 * the presentation-layer [AuditDisplay] (consumed by composables via [AuditInfoCard]).
 *
 * Intentionally a thin 1:1 field mapping with zero formatting logic — all rendering decisions
 * (timestamp formatting, null display text, "—" for migrated rows with createdAt == 0L)
 * live exclusively in the UI layer.
 */
fun AuditInfo.toDisplay(): AuditDisplay = AuditDisplay(
    createdBy = createdBy,
    createdAt = createdAt,
    updatedBy = updatedBy,
    updatedAt = updatedAt
)
