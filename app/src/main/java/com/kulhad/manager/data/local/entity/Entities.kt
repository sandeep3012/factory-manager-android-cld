package com.kulhad.manager.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.ForeignKey.Companion.RESTRICT
import androidx.room.ForeignKey.Companion.SET_DEFAULT
import androidx.room.Index
import androidx.room.PrimaryKey

// =====================================================================================
// Enums (stored as TEXT)
// =====================================================================================

enum class WorkerType { PIECE, SALARY }

enum class StockChangeType { PRODUCTION, SALE, LOSS, ADJUSTMENT }

// =====================================================================================
// 1. users
// =====================================================================================

@Entity(
    tableName = "users",
    indices = [Index(value = ["email"], unique = true)]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val email: String,
    @ColumnInfo(name = "password_hash") val passwordHash: String,
    @ColumnInfo(name = "created_at") val createdAt: Long
)

// =====================================================================================
// 2. workers
// =====================================================================================

@Entity(tableName = "workers")
data class WorkerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String,
    val address: String,
    @ColumnInfo(name = "joining_date") val joiningDate: Long,
    @ColumnInfo(name = "current_type") val currentType: String, // WorkerType.name
    @ColumnInfo(name = "daily_rate", defaultValue = "0") val dailyRate: Int = 0,
    @ColumnInfo(name = "is_active", defaultValue = "1") val isActive: Boolean = true
)

// =====================================================================================
// 3. worker_type_history
// =====================================================================================

@Entity(
    tableName = "worker_type_history",
    foreignKeys = [
        ForeignKey(
            entity = WorkerEntity::class,
            parentColumns = ["id"],
            childColumns = ["worker_id"],
            onDelete = CASCADE
        )
    ],
    indices = [Index("worker_id")]
)
data class WorkerTypeHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "worker_id") val workerId: Long,
    @ColumnInfo(name = "worker_type") val workerType: String, // WorkerType.name
    @ColumnInfo(name = "daily_rate", defaultValue = "0") val dailyRate: Int = 0,
    @ColumnInfo(name = "effective_from") val effectiveFrom: Long,
    // ── Audit columns ─────────────────────────────────────────────────────────
    @ColumnInfo(name = "audit_created_by", defaultValue = "'System'") val auditCreatedBy: String = "System",
    @ColumnInfo(name = "audit_created_at", defaultValue = "0") val auditCreatedAt: Long = 0L,
    @ColumnInfo(name = "audit_updated_by") val auditUpdatedBy: String? = null,
    @ColumnInfo(name = "audit_updated_at") val auditUpdatedAt: Long? = null
)

// =====================================================================================
// 4. products
// =====================================================================================

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "size_ml") val sizeMl: Int,
    val description: String,
    @ColumnInfo(name = "is_active", defaultValue = "1") val isActive: Boolean = true,
    // ── Product Master additions (MIGRATION_3_4) ──────────────────────────────
    /** Human-readable label shown in pickers (e.g. "80ml", "Half Litre"). Defaults to "<sizeMl>ml". */
    @ColumnInfo(name = "display_label", defaultValue = "''") val displayLabel: String = "",
    /** Controls ordering in all product pickers; lower = first. Defaults to sizeMl so existing order is preserved. */
    @ColumnInfo(name = "display_order", defaultValue = "0") val displayOrder: Int = 0,
    // Audit columns — same pattern as every other audited table.
    @ColumnInfo(name = "audit_created_by", defaultValue = "'System'") val auditCreatedBy: String = "System",
    @ColumnInfo(name = "audit_created_at", defaultValue = "0") val auditCreatedAt: Long = 0L,
    @ColumnInfo(name = "audit_updated_by") val auditUpdatedBy: String? = null,
    @ColumnInfo(name = "audit_updated_at") val auditUpdatedAt: Long? = null
)

// =====================================================================================
// 5. piece_rates
// =====================================================================================

@Entity(
    tableName = "piece_rates",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["product_id"],
            onDelete = CASCADE
        )
    ],
    indices = [Index("product_id")]
)
data class PieceRateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "product_id") val productId: Long,
    @ColumnInfo(name = "rate_per_piece") val ratePerPiece: Double,
    @ColumnInfo(name = "effective_from") val effectiveFrom: Long
)

// =====================================================================================
// 6. attendance
// =====================================================================================

@Entity(
    tableName = "attendance",
    foreignKeys = [
        ForeignKey(
            entity = WorkerEntity::class,
            parentColumns = ["id"],
            childColumns = ["worker_id"],
            onDelete = CASCADE
        )
    ],
    indices = [
        Index("worker_id"),
        Index("date"),
        Index(value = ["worker_id", "date"], unique = true)
    ]
)
data class AttendanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "worker_id") val workerId: Long,
    val date: Long,
    @ColumnInfo(name = "is_present") val isPresent: Boolean,
    // ── Audit columns ─────────────────────────────────────────────────────────
    @ColumnInfo(name = "audit_created_by", defaultValue = "'System'") val auditCreatedBy: String = "System",
    @ColumnInfo(name = "audit_created_at", defaultValue = "0") val auditCreatedAt: Long = 0L,
    @ColumnInfo(name = "audit_updated_by") val auditUpdatedBy: String? = null,
    @ColumnInfo(name = "audit_updated_at") val auditUpdatedAt: Long? = null
)

// =====================================================================================
// 7. production_entries
// =====================================================================================

@Entity(
    tableName = "production_entries",
    foreignKeys = [
        ForeignKey(
            entity = WorkerEntity::class,
            parentColumns = ["id"],
            childColumns = ["worker_id"],
            onDelete = CASCADE
        ),
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["product_id"],
            onDelete = CASCADE
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["created_by"],
            onDelete = SET_DEFAULT
        )
    ],
    indices = [Index("worker_id"), Index("product_id"), Index("created_by"), Index("date")]
)
data class ProductionEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "worker_id") val workerId: Long,
    @ColumnInfo(name = "product_id") val productId: Long,
    @ColumnInfo(name = "quantity_produced") val quantityProduced: Int,
    @ColumnInfo(name = "defective_quantity", defaultValue = "0") val defectiveQuantity: Int = 0,
    @ColumnInfo(name = "rate_snapshot") val rateSnapshot: Double,
    val date: Long,
    // Existing FK-based tracking (user ID integer) — kept for relational queries
    @ColumnInfo(name = "created_by", defaultValue = "0") val createdBy: Long,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    // ── Audit columns (TEXT display-name, survives user deletion) ─────────────
    @ColumnInfo(name = "audit_created_by", defaultValue = "'System'") val auditCreatedBy: String = "System",
    @ColumnInfo(name = "audit_created_at", defaultValue = "0") val auditCreatedAt: Long = 0L,
    @ColumnInfo(name = "audit_updated_by") val auditUpdatedBy: String? = null,
    @ColumnInfo(name = "audit_updated_at") val auditUpdatedAt: Long? = null
)

// =====================================================================================
// 8. stock_ledger
// =====================================================================================

@Entity(
    tableName = "stock_ledger",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["product_id"],
            onDelete = CASCADE
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["done_by"],
            onDelete = SET_DEFAULT
        )
    ],
    indices = [Index("product_id"), Index("done_by"), Index("timestamp")]
)
data class StockLedgerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "product_id") val productId: Long,
    @ColumnInfo(name = "quantity_change") val quantityChange: Int,
    @ColumnInfo(name = "change_type") val changeType: String, // StockChangeType.name
    @ColumnInfo(defaultValue = "") val remark: String = "",
    // Existing FK-based tracking (user ID integer) — kept for relational queries
    @ColumnInfo(name = "done_by", defaultValue = "0") val doneBy: Long,
    val timestamp: Long,
    // ── Audit columns (TEXT display-name, survives user deletion) ─────────────
    @ColumnInfo(name = "audit_created_by", defaultValue = "'System'") val auditCreatedBy: String = "System",
    @ColumnInfo(name = "audit_created_at", defaultValue = "0") val auditCreatedAt: Long = 0L,
    @ColumnInfo(name = "audit_updated_by") val auditUpdatedBy: String? = null,
    @ColumnInfo(name = "audit_updated_at") val auditUpdatedAt: Long? = null
)

// =====================================================================================
// 9. sales
// =====================================================================================

@Entity(
    tableName = "sales",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["created_by"],
            onDelete = SET_DEFAULT
        )
    ],
    indices = [Index("created_by"), Index("date")]
)
data class SaleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "customer_name") val customerName: String,
    val date: Long,
    @ColumnInfo(name = "total_amount") val totalAmount: Int,
    // Existing FK-based tracking (user ID integer) — kept for relational queries
    @ColumnInfo(name = "created_by", defaultValue = "0") val createdBy: Long,
    // ── Audit columns (TEXT display-name, survives user deletion) ─────────────
    @ColumnInfo(name = "audit_created_by", defaultValue = "'System'") val auditCreatedBy: String = "System",
    @ColumnInfo(name = "audit_created_at", defaultValue = "0") val auditCreatedAt: Long = 0L,
    @ColumnInfo(name = "audit_updated_by") val auditUpdatedBy: String? = null,
    @ColumnInfo(name = "audit_updated_at") val auditUpdatedAt: Long? = null
)

// =====================================================================================
// 10. sale_items
// =====================================================================================

@Entity(
    tableName = "sale_items",
    foreignKeys = [
        ForeignKey(
            entity = SaleEntity::class,
            parentColumns = ["id"],
            childColumns = ["sale_id"],
            onDelete = CASCADE
        ),
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["product_id"],
            onDelete = CASCADE
        )
    ],
    indices = [Index("sale_id"), Index("product_id")]
)
data class SaleItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "sale_id") val saleId: Long,
    @ColumnInfo(name = "product_id") val productId: Long,
    val quantity: Int,
    @ColumnInfo(name = "price_per_unit") val pricePerUnit: Int
)

// =====================================================================================
// 11. payments
// =====================================================================================

@Entity(
    tableName = "payments",
    foreignKeys = [
        ForeignKey(
            entity = SaleEntity::class,
            parentColumns = ["id"],
            childColumns = ["sale_id"],
            onDelete = CASCADE
        )
    ],
    indices = [Index("sale_id"), Index("date")]
)
data class PaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "sale_id") val saleId: Long,
    val amount: Int,
    val date: Long,
    @ColumnInfo(defaultValue = "") val remark: String = "",
    // ── Audit columns ─────────────────────────────────────────────────────────
    @ColumnInfo(name = "audit_created_by", defaultValue = "'System'") val auditCreatedBy: String = "System",
    @ColumnInfo(name = "audit_created_at", defaultValue = "0") val auditCreatedAt: Long = 0L,
    @ColumnInfo(name = "audit_updated_by") val auditUpdatedBy: String? = null,
    @ColumnInfo(name = "audit_updated_at") val auditUpdatedAt: Long? = null
)

// =====================================================================================
// 12. expense_types
// =====================================================================================

@Entity(
    tableName = "expense_types",
    indices = [Index(value = ["name"], unique = true)]
)
data class ExpenseTypeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "is_active", defaultValue = "1") val isActive: Boolean = true
)

// =====================================================================================
// 13. expenses
// =====================================================================================

@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = ExpenseTypeEntity::class,
            parentColumns = ["id"],
            childColumns = ["expense_type_id"],
            onDelete = RESTRICT
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["added_by"],
            onDelete = SET_DEFAULT
        )
    ],
    indices = [Index("expense_type_id"), Index("added_by"), Index("date")]
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "expense_type_id") val expenseTypeId: Long,
    val amount: Int,
    val date: Long,
    @ColumnInfo(defaultValue = "") val remark: String = "",
    @ColumnInfo(name = "added_by", defaultValue = "0") val addedBy: Long,
    // ── Audit columns (added in MIGRATION_2_3) ────────────────────────────────
    @ColumnInfo(name = "audit_created_by", defaultValue = "'System'") val auditCreatedBy: String = "System",
    @ColumnInfo(name = "audit_created_at", defaultValue = "0") val auditCreatedAt: Long = 0L,
    @ColumnInfo(name = "audit_updated_by") val auditUpdatedBy: String? = null,
    @ColumnInfo(name = "audit_updated_at") val auditUpdatedAt: Long? = null
)

// =====================================================================================
// 14. worker_advances
// =====================================================================================

@Entity(
    tableName = "worker_advances",
    foreignKeys = [
        ForeignKey(
            entity = WorkerEntity::class,
            parentColumns = ["id"],
            childColumns = ["worker_id"],
            onDelete = CASCADE
        )
    ],
    indices = [Index("worker_id"), Index("date")]
)
data class WorkerAdvanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "worker_id") val workerId: Long,
    val amount: Int,
    val date: Long,
    @ColumnInfo(defaultValue = "") val remark: String = "",
    // ── Audit columns ─────────────────────────────────────────────────────────
    @ColumnInfo(name = "audit_created_by", defaultValue = "'System'") val auditCreatedBy: String = "System",
    @ColumnInfo(name = "audit_created_at", defaultValue = "0") val auditCreatedAt: Long = 0L,
    @ColumnInfo(name = "audit_updated_by") val auditUpdatedBy: String? = null,
    @ColumnInfo(name = "audit_updated_at") val auditUpdatedAt: Long? = null
)
