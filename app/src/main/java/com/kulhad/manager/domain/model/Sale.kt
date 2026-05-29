package com.kulhad.manager.domain.model

enum class SaleStatus { PAID, PARTIAL, UNPAID }

/**
 * Domain model for a single sale record.
 *
 * [audit] carries write-audit metadata from [com.kulhad.manager.data.local.entity.SaleEntity].
 * Sales are write-once (no edit flow) so [AuditInfo.updatedBy] and [AuditInfo.updatedAt]
 * will always be null in practice. Included for architectural consistency with all other
 * audited domain models in the project.
 *
 * Note: [SaleEntity] has no remark column; the Sale domain model therefore carries none either.
 */
data class Sale(
    val id: Long,
    val customerName: String,
    val date: Long,
    val totalAmount: Int,
    val createdBy: Long,
    val audit: AuditInfo
)

data class SaleItem(
    val id: Long,
    val saleId: Long,
    val productId: Long,
    val productSize: Int,
    val quantity: Int,
    val pricePerUnit: Int
) {
    val total: Int get() = quantity * pricePerUnit
}

data class SaleSummary(
    val sale: Sale,
    val paid: Int,
    val pending: Int,
    val status: SaleStatus
)

data class SaleDetail(
    val sale: Sale,
    val items: List<SaleItem>,
    val payments: List<Payment>,
    val paid: Int,
    val pending: Int,
    val status: SaleStatus
)

/**
 * Domain model for a single payment against a sale.
 *
 * [audit] carries the write-audit metadata from the underlying
 * [com.kulhad.manager.data.local.entity.PaymentEntity] row.
 * Payments are write-once — there is no edit flow — so [AuditInfo.updatedBy] and
 * [AuditInfo.updatedAt] will always be null in practice. They are included for
 * architectural consistency and future-proofing with the rest of the audit pipeline.
 */
data class Payment(
    val id: Long,
    val saleId: Long,
    val amount: Int,
    val date: Long,
    val remark: String,
    val audit: AuditInfo
)

/** Draft used by the Create Sale screen before persisting. */
data class SaleItemDraft(
    val productId: Long,
    val productSize: Int,
    val quantity: Int,
    val pricePerUnit: Int
) {
    val total: Int get() = quantity * pricePerUnit
}
