package com.kulhad.manager.ui.screens.masters

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kulhad.manager.data.local.dao.ProductDao
import com.kulhad.manager.data.local.entity.ProductEntity
import com.kulhad.manager.data.repository.ProductionRepository
import com.kulhad.manager.domain.model.AuditInfo
import com.kulhad.manager.domain.model.PieceRate
import com.kulhad.manager.domain.model.Product
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// ── Save result ───────────────────────────────────────────────────────────────

/** Result returned by [addRate] and [updateRate]. */
sealed interface RateSaveResult {
    object Success     : RateSaveResult
    /** Rate was empty, zero, negative, or not a valid positive number. */
    object InvalidRate : RateSaveResult
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

/**
 * ViewModel for [ProductRateHistoryScreen].
 *
 * [productId] is read from [SavedStateHandle] which Navigation Compose populates
 * automatically from the `{productId}` path argument when the back-stack entry is created.
 *
 * Exposes:
 *  - [product]     — reactive product metadata for the screen header.
 *  - [rateHistory] — reactive list of all rate rows for this product, newest-first.
 *
 * Writes are routed through [ProductionRepository]:
 *  - [addRate]    — inserts a new row; the new row automatically becomes "current".
 *  - [updateRate] — in-place update of an existing row's [ratePerPiece] and audit fields.
 *                   Does NOT create a duplicate row. Does NOT touch any
 *                   [production_entries.rate_snapshot] values.
 */
@HiltViewModel
class ProductRateHistoryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val productionRepository: ProductionRepository,
    private val productDao: ProductDao
) : ViewModel() {

    val productId: Long = checkNotNull(savedStateHandle["productId"])

    // ── Reactive product header ───────────────────────────────────────────────

    val product: StateFlow<Product?> =
        productDao.observeById(productId)
            .map { entity -> entity?.toDomain() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // ── Reactive rate history ─────────────────────────────────────────────────

    /**
     * All rate rows for [productId], ordered newest-first.
     * The first entry (index 0) is the "current" active rate.
     */
    val rateHistory: StateFlow<List<PieceRate>> =
        productionRepository.observeRateHistory(productId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── Write operations ──────────────────────────────────────────────────────

    /**
     * Add a brand-new rate row with [effectiveFrom] = now.
     * Validation: [ratePerPiece] must be > 0.
     */
    fun addRate(ratePerPiece: Double, onResult: (RateSaveResult) -> Unit) {
        if (ratePerPiece <= 0.0) {
            onResult(RateSaveResult.InvalidRate)
            return
        }
        viewModelScope.launch {
            productionRepository.addRate(productId, ratePerPiece)
            onResult(RateSaveResult.Success)
        }
    }

    /**
     * In-place update of an existing rate row identified by [rateId].
     * Validation: [newRatePerPiece] must be > 0.
     */
    fun updateRate(rateId: Long, newRatePerPiece: Double, onResult: (RateSaveResult) -> Unit) {
        if (newRatePerPiece <= 0.0) {
            onResult(RateSaveResult.InvalidRate)
            return
        }
        viewModelScope.launch {
            productionRepository.updateRate(rateId, newRatePerPiece)
            onResult(RateSaveResult.Success)
        }
    }
}

// ── Local toDomain mapping ────────────────────────────────────────────────────

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
