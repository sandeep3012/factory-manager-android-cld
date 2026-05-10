package com.kulhad.manager.domain.model

data class ExpenseType(
    val id: Long,
    val name: String,
    val isActive: Boolean
)

data class Expense(
    val id: Long,
    val typeId: Long,
    val typeName: String,
    val amount: Int,
    val date: Long,
    val remark: String,
    val addedBy: Long
)
