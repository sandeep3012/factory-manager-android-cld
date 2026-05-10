package com.kulhad.manager.domain.model

data class Product(
    val id: Long,
    val sizeMl: Int,
    val description: String,
    val isActive: Boolean
)

data class ProductWithRate(
    val product: Product,
    val ratePerPiece: Double,
    val effectiveFrom: Long
)
