package com.kulhad.manager.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
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
 * DAO dedicated exclusively to backup/restore operations.
 *
 * Keeping these operations in a separate DAO prevents pollution of the 14 business DAOs
 * and makes it easy to see exactly what the backup system reads and writes.
 *
 * ── DELETE ORDER (FK-safe: children before parents) ──────────────────────────
 *  1. worker_advances, worker_type_history, attendance, production_entries
 *  2. stock_ledger, sale_items, payments, expenses
 *  3. sales, piece_rates, workers, products, expense_types, users
 *
 * ── INSERT ORDER (FK-safe: parents before children) ──────────────────────────
 *  1. users, expense_types, products, workers
 *  2. piece_rates, worker_type_history, sales
 *  3. attendance, production_entries, stock_ledger
 *  4. sale_items, payments, expenses, worker_advances
 *
 * All inserts use [OnConflictStrategy.REPLACE] — since every row was just deleted from
 * the same table, no conflict is expected.  REPLACE is used as a safety net in case
 * the backup contains duplicate IDs (which would indicate a corrupt file).
 */
@Dao
interface BackupDao {

    // ═══════════════════════════════════════════════════════════════════════
    // FULL READS — snapshot of every table for export
    // ═══════════════════════════════════════════════════════════════════════

    @Query("SELECT * FROM users")
    suspend fun getAllUsers(): List<UserEntity>

    @Query("SELECT * FROM workers")
    suspend fun getAllWorkers(): List<WorkerEntity>

    @Query("SELECT * FROM worker_type_history")
    suspend fun getAllWorkerTypeHistory(): List<WorkerTypeHistoryEntity>

    @Query("SELECT * FROM products")
    suspend fun getAllProducts(): List<ProductEntity>

    @Query("SELECT * FROM piece_rates")
    suspend fun getAllPieceRates(): List<PieceRateEntity>

    @Query("SELECT * FROM attendance")
    suspend fun getAllAttendance(): List<AttendanceEntity>

    @Query("SELECT * FROM production_entries")
    suspend fun getAllProductionEntries(): List<ProductionEntryEntity>

    @Query("SELECT * FROM stock_ledger")
    suspend fun getAllStockLedger(): List<StockLedgerEntity>

    @Query("SELECT * FROM sales")
    suspend fun getAllSales(): List<SaleEntity>

    @Query("SELECT * FROM sale_items")
    suspend fun getAllSaleItems(): List<SaleItemEntity>

    @Query("SELECT * FROM payments")
    suspend fun getAllPayments(): List<PaymentEntity>

    @Query("SELECT * FROM expense_types")
    suspend fun getAllExpenseTypes(): List<ExpenseTypeEntity>

    @Query("SELECT * FROM expenses")
    suspend fun getAllExpenses(): List<ExpenseEntity>

    @Query("SELECT * FROM worker_advances")
    suspend fun getAllWorkerAdvances(): List<WorkerAdvanceEntity>

    // ═══════════════════════════════════════════════════════════════════════
    // DELETE ALL — in FK-safe order (children before parents)
    // ═══════════════════════════════════════════════════════════════════════

    @Query("DELETE FROM worker_advances")       suspend fun deleteAllWorkerAdvances()
    @Query("DELETE FROM worker_type_history")   suspend fun deleteAllWorkerTypeHistory()
    @Query("DELETE FROM attendance")            suspend fun deleteAllAttendance()
    @Query("DELETE FROM production_entries")    suspend fun deleteAllProductionEntries()
    @Query("DELETE FROM stock_ledger")          suspend fun deleteAllStockLedger()
    @Query("DELETE FROM sale_items")            suspend fun deleteAllSaleItems()
    @Query("DELETE FROM payments")              suspend fun deleteAllPayments()
    @Query("DELETE FROM expenses")              suspend fun deleteAllExpenses()
    @Query("DELETE FROM sales")                 suspend fun deleteAllSales()
    @Query("DELETE FROM piece_rates")           suspend fun deleteAllPieceRates()
    @Query("DELETE FROM workers")               suspend fun deleteAllWorkers()
    @Query("DELETE FROM products")              suspend fun deleteAllProducts()
    @Query("DELETE FROM expense_types")         suspend fun deleteAllExpenseTypes()
    @Query("DELETE FROM users")                 suspend fun deleteAllUsers()

    // ═══════════════════════════════════════════════════════════════════════
    // INSERT ALL — in FK-safe order (parents before children)
    // ═══════════════════════════════════════════════════════════════════════

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllUsers(rows: List<UserEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllExpenseTypes(rows: List<ExpenseTypeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllProducts(rows: List<ProductEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllWorkers(rows: List<WorkerEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllPieceRates(rows: List<PieceRateEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllWorkerTypeHistory(rows: List<WorkerTypeHistoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSales(rows: List<SaleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllAttendance(rows: List<AttendanceEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllProductionEntries(rows: List<ProductionEntryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllStockLedger(rows: List<StockLedgerEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSaleItems(rows: List<SaleItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllPayments(rows: List<PaymentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllExpenses(rows: List<ExpenseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllWorkerAdvances(rows: List<WorkerAdvanceEntity>)
}
