package com.kulhad.manager.domain.model

import com.kulhad.manager.data.local.entity.WorkerType

data class Worker(
    val id: Long,
    val name: String,
    val phone: String,
    val address: String,
    val joiningDate: Long,
    val currentType: WorkerType,
    val dailyRate: Int,
    val isActive: Boolean
)

data class WorkerWithAttendance(
    val worker: Worker,
    val isPresentToday: Boolean?
)

data class WorkerTypeChange(
    val id: Long,
    val workerId: Long,
    val workerType: WorkerType,
    val dailyRate: Int,
    val effectiveFrom: Long
)

/**
 * Domain model for a single advance paid to a worker.
 *
 * [audit] carries the write-audit metadata from the underlying
 * [com.kulhad.manager.data.local.entity.WorkerAdvanceEntity] row.
 * Advances are write-once — there is no edit flow — so [AuditInfo.updatedBy] and
 * [AuditInfo.updatedAt] will always be null in practice. They are included for
 * architectural consistency with the rest of the audit pipeline.
 */
data class WorkerAdvanceRecord(
    val id: Long,
    val workerId: Long,
    val amount: Int,
    val date: Long,
    val remark: String,
    val audit: AuditInfo
)

/**
 * Thin domain wrapper for a single attendance row, used by history queries.
 *
 * [audit] carries the write-audit metadata (createdBy/At, updatedBy/At) from the DB row.
 * Convert to [com.kulhad.manager.domain.model.AuditDisplay] via
 * [com.kulhad.manager.data.util.toDisplay] before passing to composables.
 */
data class AttendanceRecord(
    val workerId: Long,
    val date: Long,
    val isPresent: Boolean,
    val audit: AuditInfo
)
