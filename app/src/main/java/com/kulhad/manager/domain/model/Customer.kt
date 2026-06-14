package com.kulhad.manager.domain.model

val CUSTOMER_TYPES = listOf("Retail", "Distributor", "Tea Stall", "Restaurant", "Wholesaler", "Other")

data class Customer(
    val id: Long,
    val customerCode: String,
    val name: String,
    val mobileNumber: String,
    val address: String,
    val customerType: String,
    val isActive: Boolean,
    val audit: AuditInfo
)

data class CustomerWithStats(
    val customer: Customer,
    val totalSales: Int,
    val totalPaid: Int,
    val outstanding: Int,
    val lastPurchaseDate: Long?
)

data class CustomerDetailData(
    val customer: Customer,
    val totalSales: Int,
    val totalPaid: Int,
    val outstanding: Int,
    val lastPurchaseDate: Long?,
    val sales: List<SaleSummary>
)
