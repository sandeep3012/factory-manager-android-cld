package com.kulhad.manager.data.repository

import com.kulhad.manager.data.local.dao.ProductDao
import com.kulhad.manager.data.local.dao.SaleDao
import com.kulhad.manager.data.local.dao.SaleItemDao
import com.kulhad.manager.data.local.dao.StockLedgerDao
import com.kulhad.manager.data.local.dao.UserDao
import com.kulhad.manager.data.local.dao.WorkerDao
import com.kulhad.manager.data.local.entity.StockChangeType
import com.kulhad.manager.data.local.entity.StockLedgerEntity
import com.kulhad.manager.data.util.DateUtils
import com.kulhad.manager.data.util.StockThresholds
import com.kulhad.manager.domain.model.StockItem
import com.kulhad.manager.domain.model.StockMovement
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

@Singleton
class StockRepository @Inject constructor(
    private val stockLedgerDao: StockLedgerDao,
    private val productDao: ProductDao,
    private val workerDao: WorkerDao,
    private val saleDao: SaleDao,
    private val saleItemDao: SaleItemDao,
    private val userDao: UserDao
) {

    fun observeStockItems(): Flow<List<StockItem>> = combine(
        productDao.observeActive(),
        stockLedgerDao.observeAllStock()
    ) { products, totals ->
        val byProduct = totals.associate { it.productId to it.qty }
        products.map { p ->
            StockItem(
                product = p.toDomain(),
                quantity = byProduct[p.id] ?: 0
            )
        }
    }

    fun observeAlertCount(): Flow<Int> =
        observeStockItems().map { items -> items.count { StockThresholds.isAlert(it.quantity) } }

    fun observeStockForProduct(productId: Long): Flow<Int> =
        stockLedgerDao.observeCurrentStock(productId)

    suspend fun currentStockFor(productId: Long): Int =
        stockLedgerDao.getCurrentStock(productId)

    suspend fun addAdjustment(
        productId: Long,
        quantityChange: Int,
        type: StockChangeType,
        remark: String,
        userId: Long
    ) {
        require(type == StockChangeType.LOSS || type == StockChangeType.ADJUSTMENT) {
            "Manual adjustments must be LOSS or ADJUSTMENT"
        }
        val signed = when (type) {
            StockChangeType.LOSS -> -kotlin.math.abs(quantityChange)
            StockChangeType.ADJUSTMENT -> quantityChange
            else -> quantityChange
        }
        stockLedgerDao.insert(
            StockLedgerEntity(
                productId = productId,
                quantityChange = signed,
                changeType = type.name,
                remark = remark,
                doneBy = userId,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    /**
     * Stream the ledger for one product, decorated with worker/customer description text
     * so the screen can render rich rows.
     */
    fun observeLedgerForProduct(productId: Long): Flow<List<StockMovement>> = combine(
        stockLedgerDao.observeForProduct(productId),
        productDao.observeActive(),
        workerDao.observeAll(),
        saleDao.observeAll(),
        userDao.observeAll()
    ) { entries, products, _, sales, users ->
        val sizeById = products.associate { it.id to it.sizeMl }
        val saleByCloseTs = sales.associateBy { it.id }
        val userById = users.associate { it.id to it.name }

        entries.map { e ->
            val size = sizeById[e.productId] ?: 0
            val type = runCatching { StockChangeType.valueOf(e.changeType) }
                .getOrDefault(StockChangeType.ADJUSTMENT)
            val description = when (type) {
                StockChangeType.PRODUCTION -> "Production"
                StockChangeType.SALE -> e.remark.ifBlank { "Sale" }
                StockChangeType.LOSS -> e.remark.ifBlank { "Loss" }
                StockChangeType.ADJUSTMENT -> e.remark.ifBlank { "Adjustment" }
            }
            StockMovement(
                id = e.id,
                productId = e.productId,
                productSize = size,
                quantityChange = e.quantityChange,
                type = type,
                remark = e.remark,
                description = description,
                doneBy = e.doneBy,
                doneByName = userById[e.doneBy] ?: "—",
                timestamp = e.timestamp
            )
        }
    }

    /** Compute running balance over the last 7 days for a single product. */
    suspend fun runningBalanceLast7Days(productId: Long): List<Pair<Long, Int>> {
        val starts = DateUtils.last7DayStarts()
        val results = mutableListOf<Pair<Long, Int>>()
        for (day in starts) {
            val until = DateUtils.endOfDay(day)
            val balance = stockLedgerDao.entriesUpTo(productId, until).sumOf { it.quantityChange }
            results.add(day to balance)
        }
        return results
    }
}
