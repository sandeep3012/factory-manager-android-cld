package com.kulhad.manager.ui.screens.masters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kulhad.manager.data.local.dao.PieceRateDao
import com.kulhad.manager.data.local.dao.ProductDao
import com.kulhad.manager.data.local.entity.PieceRateEntity
import com.kulhad.manager.data.local.entity.ProductEntity
import com.kulhad.manager.data.util.AuditUtils
import com.kulhad.manager.di.UserSessionManager
import com.kulhad.manager.domain.model.AuditInfo
import com.kulhad.manager.domain.model.Product
import com.kulhad.manager.domain.model.ProductWithRate
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// ── Save result ───────────────────────────────────────────────────────────────

/** Result returned to the UI after a save/add attempt. */
sealed interface ProductSaveResult {
    object Success : ProductSaveResult
    data class DuplicateLabel(val label: String) : ProductSaveResult
    object EmptyName   : ProductSaveResult
    object EmptyLabel  : ProductSaveResult
    /** Rate field was empty, zero, or not a valid positive number. */
    object InvalidRate : ProductSaveResult
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class ProductMasterViewModel @Inject constructor(
    private val productDao: ProductDao,
    private val pieceRateDao: PieceRateDao,
    private val userSessionManager: UserSessionManager
) : ViewModel() {

    /**
     * ALL products (active + inactive) with their current piece rate.
     *
     * Reactive on changes to BOTH `products` and `piece_rates` tables:
     *  - `productDao.observeAll()` fires on every product add/edit/deactivation.
     *  - `pieceRateDao.observeAllRates()` fires whenever any rate row is inserted
     *    (i.e. when a rate is changed via the edit dialog).
     *
     * The second argument `_` is discarded — it is only a change-notification trigger.
     * `pieceRateDao.currentRate()` is then called fresh per product so the list always
     * reflects the most recent rate even immediately after an edit.
     */
    val allProductsWithRates: StateFlow<List<ProductWithRate>> =
        combine(
            productDao.observeAll(),
            pieceRateDao.observeAllRates()
        ) { products, _ ->
            products.map { p ->
                val rate = pieceRateDao.currentRate(p.id)
                ProductWithRate(
                    product       = p.toDomain(),
                    ratePerPiece  = rate?.ratePerPiece ?: 0.0,
                    effectiveFrom = rate?.effectiveFrom ?: 0L
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── Write operations ──────────────────────────────────────────────────────

    /**
     * Add a brand-new product AND its initial piece rate.
     *
     * Validation (all checked before any DB write):
     *  - name must not be blank
     *  - displayLabel must not be blank
     *  - displayLabel must be unique (case-insensitive, across ALL products)
     *  - ratePerPiece must be a positive number > 0
     *
     * Sequence:
     *  1. Insert ProductEntity  → get new productId
     *  2. Insert PieceRateEntity(productId, ratePerPiece, now)
     *
     * The two inserts are sequential but not wrapped in a Room transaction.
     * If the rate insert fails (extremely unlikely), the product will show
     * rate = 0.0 in Production — correctable by reopening the edit dialog.
     */
    fun addProduct(
        name: String,
        displayLabel: String,
        displayOrder: Int,
        ratePerPiece: Double,
        onResult: (ProductSaveResult) -> Unit
    ) {
        viewModelScope.launch {
            val trimName  = name.trim()
            val trimLabel = displayLabel.trim()
            when {
                trimName.isBlank()  -> { onResult(ProductSaveResult.EmptyName);   return@launch }
                trimLabel.isBlank() -> { onResult(ProductSaveResult.EmptyLabel);  return@launch }
                ratePerPiece <= 0.0 -> { onResult(ProductSaveResult.InvalidRate); return@launch }
                productDao.countByLabel(trimLabel) > 0 -> {
                    onResult(ProductSaveResult.DuplicateLabel(trimLabel))
                    return@launch
                }
            }
            val audit = AuditUtils.createAudit(userSessionManager.currentUser.value)
            // Step A: insert product
            val productId = productDao.insert(
                ProductEntity(
                    sizeMl         = parseSizeMl(trimLabel),
                    description    = trimName,
                    isActive       = true,
                    displayLabel   = trimLabel,
                    displayOrder   = displayOrder,
                    auditCreatedBy = audit.createdBy,
                    auditCreatedAt = audit.createdAt
                )
            )
            // Step B: insert initial piece rate
            pieceRateDao.insert(
                PieceRateEntity(
                    productId     = productId,
                    ratePerPiece  = ratePerPiece,
                    effectiveFrom = System.currentTimeMillis()
                )
            )
            onResult(ProductSaveResult.Success)
        }
    }

    /**
     * Update an existing product's name, label, displayOrder, isActive, and optionally
     * its piece rate.
     *
     * Rate history is preserved via INSERT (not UPDATE):
     *  - If [newRatePerPiece] differs from the current stored rate, a NEW PieceRateEntity
     *    is inserted with a fresh [effectiveFrom] timestamp.
     *  - [PieceRateDao.currentRate()] always returns the row with the highest
     *    [effectiveFrom], so the new row automatically becomes "current."
     *  - All existing production_entries.rate_snapshot values remain untouched —
     *    historical earnings calculations are completely unaffected.
     *  - If [newRatePerPiece] equals the current rate, no new row is inserted.
     */
    fun updateProduct(
        id: Long,
        name: String,
        displayLabel: String,
        displayOrder: Int,
        isActive: Boolean,
        newRatePerPiece: Double,
        onResult: (ProductSaveResult) -> Unit
    ) {
        viewModelScope.launch {
            val trimName  = name.trim()
            val trimLabel = displayLabel.trim()
            when {
                trimName.isBlank()  -> { onResult(ProductSaveResult.EmptyName);   return@launch }
                trimLabel.isBlank() -> { onResult(ProductSaveResult.EmptyLabel);  return@launch }
                newRatePerPiece <= 0.0 -> { onResult(ProductSaveResult.InvalidRate); return@launch }
                productDao.countByLabel(trimLabel, excludeId = id) > 0 -> {
                    onResult(ProductSaveResult.DuplicateLabel(trimLabel))
                    return@launch
                }
            }
            val existing = productDao.findById(id) ?: return@launch
            val audit    = AuditUtils.updateAudit(
                oldCreatedBy = existing.auditCreatedBy,
                oldCreatedAt = existing.auditCreatedAt,
                currentUser  = userSessionManager.currentUser.value
            )
            val newSizeMl = parseSizeMl(trimLabel).takeIf { it > 0 } ?: existing.sizeMl

            // Update the product row (name, label, order, status, audit)
            productDao.update(
                existing.copy(
                    description    = trimName,
                    displayLabel   = trimLabel,
                    displayOrder   = displayOrder,
                    isActive       = isActive,
                    sizeMl         = newSizeMl,
                    auditUpdatedBy = audit.updatedBy,
                    auditUpdatedAt = audit.updatedAt
                )
            )

            // Conditionally insert a new rate row only if the rate actually changed
            val currentRate = pieceRateDao.currentRate(id)?.ratePerPiece ?: 0.0
            if (newRatePerPiece != currentRate) {
                pieceRateDao.insert(
                    PieceRateEntity(
                        productId     = id,
                        ratePerPiece  = newRatePerPiece,
                        effectiveFrom = System.currentTimeMillis()
                    )
                )
            }

            onResult(ProductSaveResult.Success)
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Try to extract the numeric ml value from a label like "80ml" or "80 ml".
     * Returns 0 when the label is not a simple ml-based size (e.g. "Half Litre").
     */
    private fun parseSizeMl(label: String): Int {
        val cleaned = label.trim().lowercase().removeSuffix("ml").trim()
        return cleaned.toIntOrNull() ?: 0
    }
}

// ── Local toDomain mapping ────────────────────────────────────────────────────
// Mirrors the internal extension in ProductionRepository — defined here so the
// ViewModel is self-contained without a cross-package import.

private fun ProductEntity.toDomain(): Product = Product(
    id           = id,
    sizeMl       = sizeMl,
    description  = description,
    isActive     = isActive,
    displayLabel = displayLabel.ifBlank { "${sizeMl}ml" },
    displayOrder = displayOrder,
    audit        = AuditInfo(
        createdBy = auditCreatedBy,
        createdAt = auditCreatedAt,
        updatedBy = auditUpdatedBy,
        updatedAt = auditUpdatedAt
    )
)
