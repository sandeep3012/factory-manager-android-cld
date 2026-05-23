package com.kulhad.manager.data.repository

import androidx.room.withTransaction
import com.kulhad.manager.data.local.KulhadDatabase
import com.kulhad.manager.data.local.dao.PaymentDao
import com.kulhad.manager.data.local.dao.ProductDao
import com.kulhad.manager.data.local.dao.SaleDao
import com.kulhad.manager.data.local.dao.SaleItemDao
import com.kulhad.manager.data.local.dao.StockLedgerDao
import com.kulhad.manager.data.local.entity.PaymentEntity
import com.kulhad.manager.data.local.entity.SaleEntity
import com.kulhad.manager.data.local.entity.SaleItemEntity
import com.kulhad.manager.data.local.entity.StockChangeType
import com.kulhad.manager.data.local.entity.StockLedgerEntity
import com.kulhad.manager.data.util.DateUtils
import com.kulhad.manager.domain.model.Payment
import com.kulhad.manager.domain.model.Sale
import com.kulhad.manager.domain.model.SaleDetail
import com.kulhad.manager.domain.model.SaleItem
import com.kulhad.manager.domain.model.InsufficientStockException
import com.kulhad.manager.domain.model.SaleItemDraft
import com.kulhad.manager.domain.model.SaleStatus
import com.kulhad.manager.domain.model.SaleSummary
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

@Singleton
class SaleRepository @Inject constructor(
    private val database: KulhadDatabase,
    private val saleDao: SaleDao,
    private val saleItemDao: SaleItemDao,
    private val paymentDao: PaymentDao,
    private val stockLedgerDao: StockLedgerDao,
    private val productDao: ProductDao
) {

    /**
     * Atomically:
     * 1. Validate stock for every item — throws [InsufficientStockException] if any item
     *    exceeds available stock. The whole transaction is rolled back automatically.
     * 2. Insert Sale.
     * 3. Insert each SaleItem.
     * 4. Insert one StockLedger row (-qty, SALE) per item.
     *
     * For edit-sale use cases, pass [previousQuantities] as a map of productId → old quantity.
     * Validation then only checks the *additional* stock needed (newQty - oldQty), so reducing
     * a line item never triggers a false rejection.
     */
    suspend fun createSale(
        customerName: String,
        date: Long,
        items: List<SaleItemDraft>,
        userId: Long,
        previousQuantities: Map<Long, Int> = emptyMap()
    ): Long = database.withTransaction {
        require(items.isNotEmpty()) { "Sale must have at least one item" }

        // ── Stock validation ─────────────────────────────────────────────────
        // Aggregate total requested quantity per product (handles duplicate size rows).
        val requestedByProduct: Map<Long, Int> = items
            .groupBy { it.productId }
            .mapValues { (_, drafts) -> drafts.sumOf { it.quantity } }

        requestedByProduct.forEach { (productId, totalRequested) ->
            val previousQty = previousQuantities[productId] ?: 0
            val extraNeeded  = totalRequested - previousQty   // ≤ 0 means a reduction — always safe
            if (extraNeeded > 0) {
                val available = stockLedgerDao.getCurrentStock(productId)
                if (extraNeeded > available) {
                    val product     = productDao.findById(productId)
                    val productName = if (product != null) "${product.sizeMl}ml" else "product #$productId"
                    throw InsufficientStockException(productName, available, extraNeeded)
                }
            }
        }
        // ── End validation — nothing has been written yet ────────────────────

        val total  = items.sumOf { it.total }
        val saleId = saleDao.insert(
            SaleEntity(
                customerName = customerName,
                date = DateUtils.startOfDay(date),
                totalAmount = total,
                createdBy = userId
            )
        )

        val now = System.currentTimeMillis()
        items.forEach { d ->
            saleItemDao.insert(
                SaleItemEntity(
                    saleId = saleId,
                    productId = d.productId,
                    quantity = d.quantity,
                    pricePerUnit = d.pricePerUnit
                )
            )
            stockLedgerDao.insert(
                StockLedgerEntity(
                    productId = d.productId,
                    quantityChange = -d.quantity,
                    changeType = StockChangeType.SALE.name,
                    remark = "Sale to $customerName",
                    doneBy = userId,
                    timestamp = now
                )
            )
        }
        saleId
    }

    suspend fun addPayment(saleId: Long, amount: Int, date: Long, remark: String) {
        require(amount > 0) { "Payment amount must be positive" }
        paymentDao.insert(
            PaymentEntity(
                saleId = saleId,
                amount = amount,
                date = DateUtils.startOfDay(date),
                remark = remark
            )
        )
    }

    fun observeAllSales(): Flow<List<Sale>> =
        saleDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeAllSummaries(): Flow<List<SaleSummary>> =
        combine(saleDao.observeAll(), paymentDao.observeAllSalePaid()) { sales, paid ->
            val paidById = paid.associate { it.saleId to it.paid }
            sales.map { it.toSummary(paidById[it.id] ?: 0) }
        }

    fun observePendingSummaries(): Flow<List<SaleSummary>> =
        observeAllSummaries().map { list -> list.filter { it.status != SaleStatus.PAID } }

    fun observeSaleDetail(saleId: Long): Flow<SaleDetail?> =
        combine(
            saleDao.observeById(saleId),
            saleItemDao.observeForSale(saleId),
            paymentDao.observeForSale(saleId),
            productDao.observeActive()
        ) { sale, items, payments, products ->
            if (sale == null) return@combine null
            val sizeById = products.associate { it.id to it.sizeMl }
            val paid = payments.sumOf { it.amount }
            val pending = (sale.totalAmount - paid).coerceAtLeast(0)
            SaleDetail(
                sale = sale.toDomain(),
                items = items.map { i ->
                    SaleItem(
                        id = i.id,
                        saleId = i.saleId,
                        productId = i.productId,
                        productSize = sizeById[i.productId] ?: 0,
                        quantity = i.quantity,
                        pricePerUnit = i.pricePerUnit
                    )
                },
                payments = payments.map { p ->
                    Payment(
                        id = p.id,
                        saleId = p.saleId,
                        amount = p.amount,
                        date = p.date,
                        remark = p.remark
                    )
                },
                paid = paid,
                pending = pending,
                status = statusFor(sale.totalAmount, paid)
            )
        }

    fun observeWeekSalesTotal(): Flow<Int> {
        val from = DateUtils.addDays(DateUtils.todayStart(), -6)
        val to = DateUtils.todayEnd()
        return saleDao.observeTotalInRange(from, to)
    }

    fun observeOrderCountInRange(from: Long, to: Long): Flow<Int> =
        saleDao.observeOrderCountInRange(from, to)

    fun observeTotalInRange(from: Long, to: Long): Flow<Int> =
        saleDao.observeTotalInRange(from, to)

    fun observeCollectedInRange(from: Long, to: Long): Flow<Int> =
        paymentDao.observeCollectedInRange(from, to)

    fun observeDailySales(from: Long, to: Long) = saleDao.observeDailySales(from, to)

    fun observeCustomerTotals(from: Long, to: Long) = saleDao.observeCustomerTotals(from, to)

    /** Sum across all sales of pending = total - paid. */
    fun observeTotalPending(): Flow<Int> =
        combine(saleDao.observeAll(), paymentDao.observeAllSalePaid()) { sales, paid ->
            val paidById = paid.associate { it.saleId to it.paid }
            sales.sumOf { (it.totalAmount - (paidById[it.id] ?: 0)).coerceAtLeast(0) }
        }

    fun observeTotalCollectedAllTime(): Flow<Int> =
        paymentDao.observeAllSalePaid().map { rows -> rows.sumOf { it.paid } }

    fun observeSalesToday(): Flow<Int> =
        saleDao.observeTotalInRange(DateUtils.todayStart(), DateUtils.todayEnd())

    private fun SaleEntity.toDomain(): Sale = Sale(
        id = id,
        customerName = customerName,
        date = date,
        totalAmount = totalAmount,
        createdBy = createdBy
    )

    private fun SaleEntity.toSummary(paid: Int): SaleSummary {
        val pending = (totalAmount - paid).coerceAtLeast(0)
        return SaleSummary(
            sale = toDomain(),
            paid = paid,
            pending = pending,
            status = statusFor(totalAmount, paid)
        )
    }

    private fun statusFor(total: Int, paid: Int): SaleStatus = when {
        paid <= 0 -> SaleStatus.UNPAID
        paid >= total -> SaleStatus.PAID
        else -> SaleStatus.PARTIAL
    }
}
