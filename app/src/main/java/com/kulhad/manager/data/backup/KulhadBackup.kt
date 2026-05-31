package com.kulhad.manager.data.backup

import com.kulhad.manager.data.local.entity.AttendanceEntity
import com.kulhad.manager.data.local.entity.ExpenseEntity
import com.kulhad.manager.data.local.entity.ExpenseTypeEntity
import com.kulhad.manager.data.local.entity.PaymentEntity
import com.kulhad.manager.data.local.entity.PieceRateEntity
import com.kulhad.manager.data.local.entity.ProductEntity
import com.kulhad.manager.data.local.entity.ProductionEntryEntity
import com.kulhad.manager.data.local.entity.SaleEntity
import com.kulhad.manager.data.local.entity.SaleItemEntity
import com.kulhad.manager.data.local.entity.StockLedgerEntity
import com.kulhad.manager.data.local.entity.UserEntity
import com.kulhad.manager.data.local.entity.WorkerAdvanceEntity
import com.kulhad.manager.data.local.entity.WorkerEntity
import com.kulhad.manager.data.local.entity.WorkerTypeHistoryEntity

/**
 * Current backup format version.
 * Increment this when the backup schema changes in a backward-incompatible way.
 * Restore validation rejects backups with [backupVersion] > [BACKUP_VERSION].
 */
const val BACKUP_VERSION = 1

/**
 * Top-level container for a complete Kulhad Manager database export.
 *
 * Every business table is included. All original primary-key IDs and foreign-key
 * relationships are preserved exactly — restore re-inserts rows with their original IDs.
 *
 * Audit columns (audit_created_by/at, audit_updated_by/at) are included in each entity
 * that has them; tables without audit columns (users, workers, sale_items, etc.) carry
 * all their columns as-is.
 *
 * Tables NOT included: navigation state, Compose state, WorkingDateManager state.
 *
 * Serialised with Gson — all fields are serialised using their Kotlin property names.
 * Nullable fields (String?, Long?) serialise as JSON null and are restored to null.
 */
data class KulhadBackup(
    /** Format identifier — used by restore to reject incompatible files. */
    val backupVersion: Int = BACKUP_VERSION,
    /** Wall-clock epoch-millis when the backup was created. */
    val createdAt: Long = System.currentTimeMillis(),

    // ── All 14 Room tables ────────────────────────────────────────────────────
    val users:              List<UserEntity>              = emptyList(),
    val workers:            List<WorkerEntity>            = emptyList(),
    val workerTypeHistory:  List<WorkerTypeHistoryEntity> = emptyList(),
    val products:           List<ProductEntity>           = emptyList(),
    val pieceRates:         List<PieceRateEntity>         = emptyList(),
    val attendance:         List<AttendanceEntity>        = emptyList(),
    val productionEntries:  List<ProductionEntryEntity>   = emptyList(),
    val stockLedger:        List<StockLedgerEntity>       = emptyList(),
    val sales:              List<SaleEntity>              = emptyList(),
    val saleItems:          List<SaleItemEntity>          = emptyList(),
    val payments:           List<PaymentEntity>           = emptyList(),
    val expenseTypes:       List<ExpenseTypeEntity>       = emptyList(),
    val expenses:           List<ExpenseEntity>           = emptyList(),
    val workerAdvances:     List<WorkerAdvanceEntity>     = emptyList()
)

/**
 * Lightweight summary of a parsed backup file shown in the restore confirmation dialog.
 * The full [backup] is retained in memory until the user confirms or cancels.
 */
data class BackupPreview(
    val backup:          KulhadBackup,
    val workerCount:     Int = backup.workers.size,
    val productionCount: Int = backup.productionEntries.size,
    val salesCount:      Int = backup.sales.size,
    val expenseCount:    Int = backup.expenses.size,
    val createdAt:       Long = backup.createdAt
)
