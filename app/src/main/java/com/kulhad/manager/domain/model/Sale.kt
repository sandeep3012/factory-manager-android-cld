package com.kulhad.manager.domain.model

enum class SaleStatus { PAID, PARTIAL, UNPAID }

data class Sale(
    val id: Long,
    val customerName: String,
    val date: Long,
    val totalAmount: Int,
    val createdBy: Long
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

data class Payment(
    val id: Long,
    val saleId: Long,
    val amount: Int,
    val date: Long,
    val remark: String
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
