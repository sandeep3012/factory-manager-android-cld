package com.kulhad.manager.domain.model

data class ProductionEntry(
    val id: Long,
    val workerId: Long,
    val workerName: String,
    val productId: Long,
    val productSize: Int,
    val quantityProduced: Int,
    val defectiveQuantity: Int,
    val rateSnapshot: Double,
    val date: Long,
    val createdBy: Long,
    val createdAt: Long
) {
    val netQty: Int get() = quantityProduced - defectiveQuantity
    val earnings: Double get() = netQty * rateSnapshot
}
