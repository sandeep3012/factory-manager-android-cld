package com.kulhad.manager.data.repository

import androidx.room.withTransaction
import com.kulhad.manager.data.local.KulhadDatabase
import com.kulhad.manager.data.local.dao.AttendanceDao
import com.kulhad.manager.data.local.dao.PieceRateDao
import com.kulhad.manager.data.local.dao.ProductDao
import com.kulhad.manager.data.local.dao.ProductionEntryDao
import com.kulhad.manager.data.local.dao.StockLedgerDao
import com.kulhad.manager.data.local.dao.WorkerDao
import com.kulhad.manager.data.local.entity.AttendanceEntity
import com.kulhad.manager.data.local.entity.PieceRateEntity
import com.kulhad.manager.data.local.entity.ProductEntity
import com.kulhad.manager.data.local.entity.ProductionEntryEntity
import com.kulhad.manager.data.local.entity.StockChangeType
import com.kulhad.manager.data.local.entity.StockLedgerEntity
import com.kulhad.manager.data.util.AuditUtils
import com.kulhad.manager.data.util.DateUtils
import com.kulhad.manager.di.UserSessionManager
import com.kulhad.manager.domain.model.AuditInfo
import com.kulhad.manager.domain.model.PieceRate
import com.kulhad.manager.domain.model.Product
import com.kulhad.manager.domain.model.ProductWithRate
import com.kulhad.manager.domain.model.ProductionEntry
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

@Singleton
class ProductionRepository @Inject constructor(
    private val database: KulhadDatabase,
    private val productDao: ProductDao,
    private val pieceRateDao: PieceRateDao,
    private val productionDao: ProductionEntryDao,
    private val stockLedgerDao: StockLedgerDao,
    private val workerDao: WorkerDao,
    private val attendanceDao: AttendanceDao,
    private val userSessionManager: UserSessionManager
) {

    fun observeProducts(): Flow<List<Product>> =
        productDao.observeActive().map { list -> list.map { it.toDomain() } }

    /**
     * All products (active **and** inactive), ordered by display_order ASC, size_ml ASC.
     *
     * Used by historical / reporting screens (Production History, Stock Ledger, Adjustment
     * History) that need to resolve product names for entries whose product has since been
     * deactivated. Using [observeProducts] (active-only) in those contexts causes deactivated
     * products to resolve as "0ml" / blank — this method prevents that.
     *
     * Callers that show a product **picker** should still use [observeProducts] / [observeActive].
     */
    fun observeAllProducts(): Flow<List<Product>> =
        productDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeProductsWithRates(): Flow<List<ProductWithRate>> =
        productDao.observeActive().map { products ->
            products.map { p ->
                val rate = pieceRateDao.currentRate(p.id)
                ProductWithRate(
                    product = p.toDomain(),
                    ratePerPiece = rate?.ratePerPiece ?: 0.0,
                    effectiveFrom = rate?.effectiveFrom ?: 0L
                )
            }
        }

    suspend fun currentRate(productId: Long): Double =
        pieceRateDao.currentRate(productId)?.ratePerPiece ?: 0.0

    fun observeCurrentRate(productId: Long): Flow<Double> =
        pieceRateDao.observeCurrentRate(productId).map { it?.ratePerPiece ?: 0.0 }

    /**
     * Insert a new piece-rate row for [productId].
     *
     * Used by [setRate] (legacy call site) and [addRate] (Rate History screen).
     * Both stamp full audit fields — existing rows from before v5 will show "—" in
     * [AuditInfoCard] for the createdAt timestamp (migrated-row sentinel 0L).
     */
    suspend fun setRate(productId: Long, rate: Double, effectiveFrom: Long) {
        val audit = AuditUtils.createAudit(userSessionManager.currentUser.value)
        pieceRateDao.insert(
            PieceRateEntity(
                productId      = productId,
                ratePerPiece   = rate,
                effectiveFrom  = effectiveFrom,
                auditCreatedBy = audit.createdBy,
                auditCreatedAt = audit.createdAt
            )
        )
    }

    // ── Rate History ──────────────────────────────────────────────────────────

    /**
     * Observe the complete rate history for [productId], newest-first.
     * Used by [ProductRateHistoryViewModel] to power the history list.
     */
    fun observeRateHistory(productId: Long): Flow<List<PieceRate>> =
        pieceRateDao.observeHistory(productId).map { list -> list.map { it.toDomain() } }

    /**
     * Insert a brand-new piece-rate row with the current wall-clock time as [effectiveFrom].
     * The new row automatically becomes the "current" rate (highest effective_from wins).
     */
    suspend fun addRate(productId: Long, ratePerPiece: Double) {
        val audit = AuditUtils.createAudit(userSessionManager.currentUser.value)
        pieceRateDao.insert(
            PieceRateEntity(
                productId      = productId,
                ratePerPiece   = ratePerPiece,
                effectiveFrom  = System.currentTimeMillis(),
                auditCreatedBy = audit.createdBy,
                auditCreatedAt = audit.createdAt
            )
        )
    }

    /**
     * Update an existing rate row in-place.
     *
     * This preserves the original [effectiveFrom] and creation audit fields.
     * Only [ratePerPiece], [auditUpdatedBy], and [auditUpdatedAt] are changed.
     *
     * Does NOT affect any [production_entries.rate_snapshot] values — existing
     * production entries always keep the rate that was current when they were created.
     */
    suspend fun updateRate(rateId: Long, newRatePerPiece: Double) {
        val existing = pieceRateDao.findById(rateId) ?: return
        val audit = AuditUtils.updateAudit(
            oldCreatedBy = existing.auditCreatedBy,
            oldCreatedAt = existing.auditCreatedAt,
            currentUser  = userSessionManager.currentUser.value
        )
        pieceRateDao.update(
            existing.copy(
                ratePerPiece   = newRatePerPiece,
                auditUpdatedBy = audit.updatedBy,
                auditUpdatedAt = audit.updatedAt
            )
        )
    }

    /**
     * Atomically:
     * 1. Insert ProductionEntry (with rate snapshot).
     * 2. Insert StockLedger row (+ net qty, type=PRODUCTION).
     * 3. Ensure attendance row exists for worker on that day (auto-mark present).
     */
    suspend fun addEntry(
        workerId: Long,
        productId: Long,
        quantity: Int,
        defective: Int,
        date: Long,
        userId: Long
    ): Long = database.withTransaction {
        require(quantity >= 0) { "Quantity cannot be negative" }
        require(defective in 0..quantity) { "Defective must be between 0 and quantity" }

        val rate = pieceRateDao.currentRate(productId)?.ratePerPiece ?: 0.0
        val day = DateUtils.startOfDay(date)
        val now = System.currentTimeMillis()
        val net = quantity - defective
        val audit = AuditUtils.createAudit(userSessionManager.currentUser.value)

        val entryId = productionDao.insert(
            ProductionEntryEntity(
                workerId          = workerId,
                productId         = productId,
                quantityProduced  = quantity,
                defectiveQuantity = defective,
                rateSnapshot      = rate,
                date              = day,
                createdBy         = userId,
                createdAt         = now,
                auditCreatedBy    = audit.createdBy,
                auditCreatedAt    = audit.createdAt
            )
        )

        if (net > 0) {
            stockLedgerDao.insert(
                StockLedgerEntity(
                    productId      = productId,
                    quantityChange = net,
                    changeType     = StockChangeType.PRODUCTION.name,
                    remark         = "",
                    doneBy         = userId,
                    timestamp      = now,
                    auditCreatedBy = audit.createdBy,
                    auditCreatedAt = audit.createdAt
                )
            )
        }

        // Auto-attendance: reuse the same audit snapshot created above so the
        // attendance row's audit_created_by / audit_created_at align with the
        // production entry that triggered it.
        val existing = attendanceDao.findByWorkerAndDate(workerId, day)
        if (existing == null) {
            attendanceDao.upsert(
                AttendanceEntity(
                    workerId       = workerId,
                    date           = day,
                    isPresent      = true,
                    auditCreatedBy = audit.createdBy,
                    auditCreatedAt = audit.createdAt
                )
            )
        }

        entryId
    }

    /**
     * Returns the enriched domain model for a single entry, or null if not found.
     * Used to pre-populate the edit form.
     */
    suspend fun findEntryById(id: Long): ProductionEntry? {
        val entity = productionDao.findById(id) ?: return null
        val worker  = workerDao.findById(entity.workerId)
        val product = productDao.findById(entity.productId)
        return ProductionEntry(
            id                = entity.id,
            workerId          = entity.workerId,
            workerName        = worker?.name ?: "Unknown",
            productId         = entity.productId,
            productSize       = product?.sizeMl ?: 0,
            productLabel      = product?.let {
                it.displayLabel.ifBlank { "${it.sizeMl}ml" }
            } ?: "${entity.productId}",
            quantityProduced  = entity.quantityProduced,
            defectiveQuantity = entity.defectiveQuantity,
            rateSnapshot      = entity.rateSnapshot,
            date              = entity.date,
            createdBy         = entity.createdBy,
            createdAt         = entity.createdAt,
            audit             = AuditInfo(
                createdBy = entity.auditCreatedBy,
                createdAt = entity.auditCreatedAt,
                updatedBy = entity.auditUpdatedBy,
                updatedAt = entity.auditUpdatedAt
            )
        )
    }

    /**
     * Updates quantity fields on an existing production entry.
     *
     * Only [quantityProduced] and [defectiveQuantity] are changed.
     * Worker, product, date, and rate_snapshot are preserved exactly as originally
     * recorded — they are locked to maintain historical accuracy.
     *
     * Stock ledger: a delta correction row is appended when net qty changes
     * (delta = newNet − oldNet). The original ledger row is never modified.
     *
     * Attendance: not touched — worker and date are unchanged, so no attendance
     * side-effect is needed or desired.
     */
    suspend fun updateEntry(
        entryId:   Long,
        quantity:  Int,
        defective: Int,
        userId:    Long
    ) = database.withTransaction {
        require(quantity >= 0)            { "Quantity cannot be negative" }
        require(defective in 0..quantity) { "Defective must be between 0 and quantity" }

        val existing = productionDao.findById(entryId) ?: return@withTransaction
        val now   = System.currentTimeMillis()
        val audit = AuditUtils.updateAudit(
            oldCreatedBy = existing.auditCreatedBy,
            oldCreatedAt = existing.auditCreatedAt,
            currentUser  = userSessionManager.currentUser.value
        )

        productionDao.update(
            existing.copy(
                quantityProduced  = quantity,
                defectiveQuantity = defective,
                auditUpdatedBy    = audit.updatedBy,
                auditUpdatedAt    = audit.updatedAt
                // workerId, productId, date, rateSnapshot — intentionally unchanged
            )
        )

        // Append a stock-ledger correction row if net qty changed
        val oldNet = existing.quantityProduced - existing.defectiveQuantity
        val newNet = quantity - defective
        val delta  = newNet - oldNet
        if (delta != 0) {
            stockLedgerDao.insert(
                StockLedgerEntity(
                    productId      = existing.productId,
                    quantityChange = delta,
                    changeType     = StockChangeType.PRODUCTION.name,
                    remark         = "Entry correction",
                    doneBy         = userId,
                    timestamp      = now,
                    auditCreatedBy = audit.createdBy,
                    auditCreatedAt = audit.createdAt
                )
            )
        }
    }

    fun observeAllEntries(): Flow<List<ProductionEntry>> = combineEntries(productionDao.observeAll())

    fun observeEntriesInRange(from: Long, to: Long): Flow<List<ProductionEntry>> =
        combineEntries(productionDao.observeInRange(from, to))

    /**
     * Reactive list of entries for a single calendar day, newest first.
     *
     * [date] is normalised to start-of-day before querying so that any epoch-millis
     * value within the day (e.g. one returned directly from WorkingDateManager) works
     * correctly without the caller performing the normalisation itself.
     *
     * Used by [com.kulhad.manager.ui.screens.production.ProductionViewModel.historyDayEntries]
     * to power the date-based Production History screen.
     */
    fun observeEntriesForDay(date: Long): Flow<List<ProductionEntry>> {
        val start = DateUtils.startOfDay(date)
        val end   = DateUtils.endOfDay(start)
        return observeEntriesInRange(start, end)
    }

    fun observeEntriesForWorkerInRange(workerId: Long, from: Long, to: Long): Flow<List<ProductionEntry>> =
        combineEntries(productionDao.observeForWorkerInRange(workerId, from, to))

    private fun combineEntries(
        source: Flow<List<ProductionEntryEntity>>
    ): Flow<List<ProductionEntry>> = combine(
        source,
        workerDao.observeAll(),
        // Use ALL products (active + inactive) so that historical entries whose product has
        // been deactivated still resolve to the correct size/label instead of falling back
        // to the default value of 0 (which was showing as "0ml" in Production History).
        productDao.observeAll()
    ) { entries, workers, products ->
        val workerById  = workers.associate { it.id to it.name }
        val productById = products.associateBy { it.id }
        entries.map { e ->
            val product = productById[e.productId]
            ProductionEntry(
                id                = e.id,
                workerId          = e.workerId,
                workerName        = workerById[e.workerId] ?: "Unknown",
                productId         = e.productId,
                productSize       = product?.sizeMl ?: 0,
                productLabel      = if (product != null)
                                        product.displayLabel.ifBlank { "${product.sizeMl}ml" }
                                    else "${e.productId}",
                quantityProduced  = e.quantityProduced,
                defectiveQuantity = e.defectiveQuantity,
                rateSnapshot      = e.rateSnapshot,
                date              = e.date,
                createdBy         = e.createdBy,
                createdAt         = e.createdAt,
                audit             = AuditInfo(
                    createdBy = e.auditCreatedBy,
                    createdAt = e.auditCreatedAt,
                    updatedBy = e.auditUpdatedBy,
                    updatedAt = e.auditUpdatedAt
                )
            )
        }
    }

    fun observeNetQtyToday(): Flow<Int> =
        productionDao.observeNetQtyInRange(DateUtils.todayStart(), DateUtils.todayEnd())

    fun observeNetQtyInRange(from: Long, to: Long): Flow<Int> =
        productionDao.observeNetQtyInRange(from, to)

    fun observeTotalQtyInRange(from: Long, to: Long): Flow<Int> =
        productionDao.observeTotalQtyInRange(from, to)

    fun observeDefectiveQtyInRange(from: Long, to: Long): Flow<Int> =
        productionDao.observeDefectiveQtyInRange(from, to)

    fun observeLaborCostInRange(from: Long, to: Long): Flow<Double> =
        productionDao.observeLaborCostInRange(from, to)

    fun observeDailyInRange(from: Long, to: Long) =
        productionDao.observeDailyInRange(from, to)

    fun observeByProductInRange(from: Long, to: Long) =
        productionDao.observeByProductInRange(from, to)

    fun observeByWorkerInRange(from: Long, to: Long) =
        productionDao.observeByWorkerInRange(from, to)

    suspend fun earningsForWorkerInRange(workerId: Long, from: Long, to: Long): Double =
        productionDao.earningsForWorkerInRange(workerId, from, to)

    suspend fun netQtyForWorkerInRange(workerId: Long, from: Long, to: Long): Int =
        productionDao.netQtyForWorkerInRange(workerId, from, to)
}

internal fun PieceRateEntity.toDomain(): PieceRate = PieceRate(
    id            = id,
    productId     = productId,
    ratePerPiece  = ratePerPiece,
    effectiveFrom = effectiveFrom,
    audit         = AuditInfo(
        createdBy = auditCreatedBy,
        createdAt = auditCreatedAt,
        updatedBy = auditUpdatedBy,
        updatedAt = auditUpdatedAt
    )
)

internal fun ProductEntity.toDomain(): Product = Product(
    id           = id,
    sizeMl       = sizeMl,
    description  = description,
    isActive     = isActive,
    displayLabel = displayLabel.ifBlank { "${sizeMl}ml" },  // fallback for migrated rows
    displayOrder = displayOrder,
    audit        = AuditInfo(
        createdBy = auditCreatedBy,
        createdAt = auditCreatedAt,
        updatedBy = auditUpdatedBy,
        updatedAt = auditUpdatedAt
    )
)
