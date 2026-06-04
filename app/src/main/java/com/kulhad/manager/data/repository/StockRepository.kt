package com.kulhad.manager.data.repository

import com.kulhad.manager.data.local.dao.ProductDao
import com.kulhad.manager.data.local.dao.SaleDao
import com.kulhad.manager.data.local.dao.SaleItemDao
import com.kulhad.manager.data.local.dao.StockLedgerDao
import com.kulhad.manager.data.local.dao.UserDao
import com.kulhad.manager.data.local.dao.WorkerDao
import com.kulhad.manager.data.local.entity.StockChangeType
import com.kulhad.manager.data.local.entity.StockLedgerEntity
import com.kulhad.manager.data.util.AuditUtils
import com.kulhad.manager.data.util.DateUtils
import com.kulhad.manager.data.util.StockThresholds
import com.kulhad.manager.di.UserSessionManager
import com.kulhad.manager.domain.model.AuditInfo
import com.kulhad.manager.domain.model.Product
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
    private val userDao: UserDao,
    private val userSessionManager: UserSessionManager
) {

    fun observeStockItems(): Flow<List<StockItem>> = combine(
        // ALL products — filtering happens below so that inactive products with remaining
        // physical stock are still visible and counted in alerts.
        productDao.observeAll(),
        stockLedgerDao.observeAllStock()
    ) { products, totals ->
        val byProduct = totals.associate { it.productId to it.qty }
        products
            .filter { p ->
                // Always show active products — operator may still need to adjust them.
                // Show inactive products only when physical stock remains (qty ≠ 0);
                // those pieces exist in the factory and need to be depleted, sold, or
                // written off.  Inactive products at exactly zero are hidden — they are
                // fully retired and have no further operational relevance.
                p.isActive || (byProduct[p.id] ?: 0) != 0
            }
            .map { p ->
                StockItem(
                    product  = p.toDomain(),
                    quantity = byProduct[p.id] ?: 0
                )
            }
    }

    fun observeAlertCount(): Flow<Int> =
        observeStockItems().map { items -> items.count { StockThresholds.isAlert(it.quantity) } }

    /**
     * Products that are valid targets for a manual stock adjustment (LOSS or ADJUSTMENT).
     *
     * Rule — include a product when:
     *  • it is **active** (normal operational product), OR
     *  • it is **inactive but still has physical stock > 0** (operator needs to write off,
     *    correct a count, or mark breakages before the product is fully retired).
     *
     * Inactive products at exactly zero stock are excluded — there is nothing to adjust
     * and showing them would clutter the picker with fully-retired entries.
     *
     * This rule mirrors the filter in [observeStockItems] so the stock-screen list and the
     * adjustment picker always agree on which products are "in play".
     */
    fun observeAdjustableProducts(): Flow<List<Product>> = combine(
        productDao.observeAll(),
        stockLedgerDao.observeAllStock()
    ) { products, totals ->
        val byProduct = totals.associate { it.productId to it.qty }
        products
            .filter { p -> p.isActive || (byProduct[p.id] ?: 0) != 0 }
            .map    { p -> p.toDomain() }
    }

    fun observeStockForProduct(productId: Long): Flow<Int> =
        stockLedgerDao.observeCurrentStock(productId)

    suspend fun currentStockFor(productId: Long): Int =
        stockLedgerDao.getCurrentStock(productId)

    /**
     * Inserts a new manual stock adjustment.
     *
     * [date] is the working-date epoch-millis from [WorkingDateManager.currentEpochMilli].
     * It is normalised to start-of-day and stored as [StockLedgerEntity.timestamp] — the
     * business/effective date of the adjustment.
     *
     * [auditCreatedAt] is always [System.currentTimeMillis] (via [AuditUtils.createAudit]),
     * preserving the actual wall-clock time of the write independently from the business date.
     *
     * Default of [DateUtils.todayStart] keeps callers that don't yet pass [date] compiling.
     */
    suspend fun addAdjustment(
        productId: Long,
        quantityChange: Int,
        type: StockChangeType,
        remark: String,
        userId: Long,
        date: Long = DateUtils.todayStart()
    ) {
        require(type == StockChangeType.LOSS || type == StockChangeType.ADJUSTMENT) {
            "Manual adjustments must be LOSS or ADJUSTMENT"
        }
        val signed = when (type) {
            StockChangeType.LOSS -> -kotlin.math.abs(quantityChange)
            StockChangeType.ADJUSTMENT -> quantityChange
            else -> quantityChange
        }
        val audit = AuditUtils.createAudit(userSessionManager.currentUser.value)
        stockLedgerDao.insert(
            StockLedgerEntity(
                productId      = productId,
                quantityChange = signed,
                changeType     = type.name,
                remark         = remark,
                doneBy         = userId,
                timestamp      = DateUtils.startOfDay(date),   // business date
                auditCreatedBy = audit.createdBy,
                auditCreatedAt = audit.createdAt               // actual write time
            )
        )
    }

    /**
     * Updates the [quantityChange] and [remark] of an existing LOSS or ADJUSTMENT row.
     *
     * Preserves [StockLedgerEntity.timestamp], [StockLedgerEntity.changeType], and all
     * creation-audit fields. Stamps [auditUpdatedBy] / [auditUpdatedAt] via
     * [AuditUtils.updateAudit]. No new row is created — this is a true UPDATE.
     */
    suspend fun updateAdjustment(id: Long, quantityChange: Int, remark: String) {
        val existing = stockLedgerDao.findById(id) ?: return
        val audit = AuditUtils.updateAudit(
            oldCreatedBy  = existing.auditCreatedBy,
            oldCreatedAt  = existing.auditCreatedAt,
            currentUser   = userSessionManager.currentUser.value
        )
        stockLedgerDao.update(
            existing.copy(
                quantityChange = quantityChange,
                remark         = remark,
                auditUpdatedBy = audit.updatedBy,
                auditUpdatedAt = audit.updatedAt
            )
        )
    }

    /**
     * Stream the ledger for one product, decorated with worker/customer description text
     * so the screen can render rich rows.
     *
     * Uses ALL products (active + inactive) so the product label in the ledger title
     * remains correct even after the product is deactivated.
     */
    fun observeLedgerForProduct(productId: Long): Flow<List<StockMovement>> = combine(
        stockLedgerDao.observeForProduct(productId),
        productDao.observeAll(),       // ALL products — history must survive deactivation
        workerDao.observeAll(),
        saleDao.observeAll(),
        userDao.observeAll()
    ) { entries, products, _, sales, users ->
        val productById = products.associateBy { it.id }
        val saleByCloseTs = sales.associateBy { it.id }
        val userById = users.associate { it.id to it.name }

        entries.map { e ->
            val product = productById[e.productId]
            val size    = product?.sizeMl ?: 0
            val label   = if (product != null) product.displayLabel.ifBlank { "${product.sizeMl}ml" }
                          else "${e.productId}"
            val type = runCatching { StockChangeType.valueOf(e.changeType) }
                .getOrDefault(StockChangeType.ADJUSTMENT)
            val description = when (type) {
                StockChangeType.PRODUCTION -> "Production"
                StockChangeType.SALE -> e.remark.ifBlank { "Sale" }
                StockChangeType.LOSS -> e.remark.ifBlank { "Loss" }
                StockChangeType.ADJUSTMENT -> e.remark.ifBlank { "Adjustment" }
            }
            StockMovement(
                id             = e.id,
                productId      = e.productId,
                productSize    = size,
                productLabel   = label,
                quantityChange = e.quantityChange,
                type           = type,
                remark         = e.remark,
                description    = description,
                doneBy         = e.doneBy,
                doneByName     = userById[e.doneBy] ?: "—",
                timestamp      = e.timestamp,
                audit          = AuditInfo(
                    createdBy = e.auditCreatedBy,
                    createdAt = e.auditCreatedAt,
                    updatedBy = e.auditUpdatedBy,
                    updatedAt = e.auditUpdatedAt
                )
            )
        }
    }

    /**
     * Reactive list of LOSS and ADJUSTMENT entries whose business-date [timestamp] falls
     * on [date]'s calendar day. Reacts automatically to insertions and edits.
     *
     * [date] is normalised to start-of-day / end-of-day internally so any epoch-millis
     * value within the day (e.g. from [WorkingDateManager.currentEpochMilli]) works.
     *
     * Powered by [StockLedgerDao.observeAdjustmentsInRange] — only LOSS/ADJUSTMENT
     * change_types are included; PRODUCTION and SALE rows are excluded.
     */
    fun observeAdjustmentsForDay(date: Long): Flow<List<StockMovement>> {
        val start = DateUtils.startOfDay(date)
        val end   = DateUtils.endOfDay(start)
        return combine(
            stockLedgerDao.observeAdjustmentsInRange(start, end),
            // ALL products — history rows for deactivated products must still show
            // the correct label (not "0ml") in the Adjustment History screen.
            productDao.observeAll()
        ) { entries, products ->
            val productById = products.associateBy { it.id }
            entries.map { e ->
                val product = productById[e.productId]
                val size    = product?.sizeMl ?: 0
                val label   = product?.displayLabel?.ifBlank { "${product.sizeMl}ml" }
                                  ?: "${e.productId}"
                val type = runCatching { StockChangeType.valueOf(e.changeType) }
                    .getOrDefault(StockChangeType.ADJUSTMENT)
                StockMovement(
                    id             = e.id,
                    productId      = e.productId,
                    productSize    = size,
                    productLabel   = label,
                    quantityChange = e.quantityChange,
                    type           = type,
                    remark         = e.remark,
                    description    = if (type == StockChangeType.LOSS) "Loss" else "Adjustment",
                    doneBy         = e.doneBy,
                    doneByName     = "—",
                    timestamp      = e.timestamp,
                    audit          = AuditInfo(
                        createdBy = e.auditCreatedBy,
                        createdAt = e.auditCreatedAt,
                        updatedBy = e.auditUpdatedBy,
                        updatedAt = e.auditUpdatedAt
                    )
                )
            }
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
