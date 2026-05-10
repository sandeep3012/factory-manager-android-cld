package com.kulhad.manager.domain.model

import com.kulhad.manager.data.local.entity.StockChangeType

data class StockItem(
    val product: Product,
    val quantity: Int
)

data class StockMovement(
    val id: Long,
    val productId: Long,
    val productSize: Int,
    val quantityChange: Int,
    val type: StockChangeType,
    val remark: String,
    val description: String,
    val doneBy: Long,
    val doneByName: String,
    val timestamp: Long
)
