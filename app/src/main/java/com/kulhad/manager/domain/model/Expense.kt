package com.kulhad.manager.domain.model

data class ExpenseType(
    val id: Long,
    val name: String,
    val isActive: Boolean
)

/**
 * Domain model for a single expense record, enriched with the category name.
 *
 * [audit] carries write-audit metadata from [com.kulhad.manager.data.local.entity.ExpenseEntity].
 * Expenses are editable (amount, remark, type); [AuditInfo.updatedBy] / [AuditInfo.updatedAt]
 * will be non-null after the first edit.
 *
 * For rows migrated before MIGRATION_2_3 (auditCreatedAt == 0L), [AuditInfoCard] renders
 * the createdAt field as "—" rather than an epoch date.
 */
data class Expense(
    val id: Long,
    val typeId: Long,
    val typeName: String,
    val amount: Int,
    val date: Long,
    val remark: String,
    val addedBy: Long,
    val audit: AuditInfo          // same package — no import needed
)
