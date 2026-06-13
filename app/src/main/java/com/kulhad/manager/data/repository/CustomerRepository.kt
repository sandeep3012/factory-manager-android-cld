package com.kulhad.manager.data.repository

import androidx.room.withTransaction
import com.kulhad.manager.data.local.KulhadDatabase
import com.kulhad.manager.data.local.dao.CustomerDao
import com.kulhad.manager.data.local.dao.PaymentDao
import com.kulhad.manager.data.local.dao.SaleDao
import com.kulhad.manager.data.local.entity.CustomerEntity
import com.kulhad.manager.data.local.entity.SaleEntity
import com.kulhad.manager.data.util.AuditUtils
import com.kulhad.manager.di.UserSessionManager
import com.kulhad.manager.domain.model.AuditInfo
import com.kulhad.manager.domain.model.Customer
import com.kulhad.manager.domain.model.CustomerDetailData
import com.kulhad.manager.domain.model.CustomerWithStats
import com.kulhad.manager.domain.model.Sale
import com.kulhad.manager.domain.model.SaleStatus
import com.kulhad.manager.domain.model.SaleSummary
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@Singleton
class CustomerRepository @Inject constructor(
    private val database: KulhadDatabase,
    private val customerDao: CustomerDao,
    private val saleDao: SaleDao,
    private val paymentDao: PaymentDao,
    private val userSessionManager: UserSessionManager
) {

    fun observeActive(): Flow<List<Customer>> =
        customerDao.observeActive().map { list -> list.map { it.toDomain() } }

    fun observeAllWithStats(includeInactive: Boolean = false): Flow<List<CustomerWithStats>> {
        val customersFlow = if (includeInactive) customerDao.observeAll() else customerDao.observeActive()
        return combine(
            customersFlow,
            saleDao.observeAll(),
            paymentDao.observeAllSalePaid()
        ) { customers, sales, allPaid ->
            val paidById = allPaid.associate { it.saleId to it.paid }
            val salesByCustomer = sales.groupBy { it.customerId }
            customers.map { c ->
                val custSales = salesByCustomer[c.id] ?: emptyList()
                val totalSales = custSales.sumOf { it.totalAmount }
                val totalPaid = custSales.sumOf { paidById[it.id] ?: 0 }
                CustomerWithStats(
                    customer = c.toDomain(),
                    totalSales = totalSales,
                    totalPaid = totalPaid,
                    outstanding = (totalSales - totalPaid).coerceAtLeast(0),
                    lastPurchaseDate = custSales.maxOfOrNull { it.date }
                )
            }
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun observeCustomerDetail(customerId: Long): Flow<CustomerDetailData?> =
        customerDao.observeById(customerId).flatMapLatest { customer ->
            if (customer == null) return@flatMapLatest flowOf(null)
            combine(
                saleDao.observeForCustomer(customerId),
                paymentDao.observeAllSalePaid()
            ) { sales, allPaid ->
                val paidById = allPaid.associate { it.saleId to it.paid }
                val totalSales = sales.sumOf { it.totalAmount }
                val totalPaid = sales.sumOf { paidById[it.id] ?: 0 }
                val outstanding = (totalSales - totalPaid).coerceAtLeast(0)
                val summaries = sales.map { s ->
                    val paid = paidById[s.id] ?: 0
                    val pending = (s.totalAmount - paid).coerceAtLeast(0)
                    SaleSummary(
                        sale = s.toDomain(customer),
                        paid = paid,
                        pending = pending,
                        status = when {
                            paid <= 0 -> SaleStatus.UNPAID
                            paid >= s.totalAmount -> SaleStatus.PAID
                            else -> SaleStatus.PARTIAL
                        }
                    )
                }
                CustomerDetailData(
                    customer = customer.toDomain(),
                    totalSales = totalSales,
                    totalPaid = totalPaid,
                    outstanding = outstanding,
                    lastPurchaseDate = sales.maxOfOrNull { it.date },
                    sales = summaries
                )
            }
        }

    suspend fun findById(id: Long): Customer? = customerDao.findById(id)?.toDomain()

    suspend fun addCustomer(
        name: String,
        phone: String,
        address: String,
        customerType: String
    ): Long = database.withTransaction {
        val audit = AuditUtils.createAudit(userSessionManager.currentUser.value)
        val id = customerDao.insert(
            CustomerEntity(
                name = name.trim(),
                mobileNumber = phone.trim(),
                address = address.trim(),
                customerType = customerType,
                auditCreatedBy = audit.createdBy,
                auditCreatedAt = audit.createdAt
            )
        )
        customerDao.updateCode(id, "CUS-%04d".format(id))
        id
    }

    suspend fun updateCustomer(
        id: Long,
        name: String,
        phone: String,
        address: String,
        customerType: String
    ) {
        val existing = customerDao.findById(id) ?: return
        val audit = AuditUtils.updateAudit(
            existing.auditCreatedBy, existing.auditCreatedAt,
            userSessionManager.currentUser.value
        )
        customerDao.update(
            existing.copy(
                name = name.trim(),
                mobileNumber = phone.trim(),
                address = address.trim(),
                customerType = customerType,
                auditUpdatedBy = audit.updatedBy,
                auditUpdatedAt = audit.updatedAt
            )
        )
    }

    suspend fun deactivateCustomer(id: Long) {
        val existing = customerDao.findById(id) ?: return
        val audit = AuditUtils.updateAudit(
            existing.auditCreatedBy, existing.auditCreatedAt,
            userSessionManager.currentUser.value
        )
        customerDao.update(existing.copy(isActive = false, auditUpdatedBy = audit.updatedBy, auditUpdatedAt = audit.updatedAt))
    }

    suspend fun reactivateCustomer(id: Long) {
        val existing = customerDao.findById(id) ?: return
        val audit = AuditUtils.updateAudit(
            existing.auditCreatedBy, existing.auditCreatedAt,
            userSessionManager.currentUser.value
        )
        customerDao.update(existing.copy(isActive = true, auditUpdatedBy = audit.updatedBy, auditUpdatedAt = audit.updatedAt))
    }

    private fun CustomerEntity.toDomain(): Customer = Customer(
        id = id,
        customerCode = customerCode,
        name = name,
        mobileNumber = mobileNumber,
        address = address,
        customerType = customerType,
        isActive = isActive,
        audit = AuditInfo(auditCreatedBy, auditCreatedAt, auditUpdatedBy, auditUpdatedAt)
    )

    private fun SaleEntity.toDomain(customer: CustomerEntity): Sale = Sale(
        id = id,
        customerId = customerId,
        customerName = customer.name,
        customerPhone = customer.mobileNumber,
        date = date,
        totalAmount = totalAmount,
        createdBy = createdBy,
        audit = AuditInfo(auditCreatedBy, auditCreatedAt, auditUpdatedBy, auditUpdatedAt)
    )
}
