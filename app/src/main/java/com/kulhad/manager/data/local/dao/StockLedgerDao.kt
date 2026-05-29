package com.kulhad.manager.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kulhad.manager.data.local.entity.StockLedgerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StockLedgerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: StockLedgerEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<StockLedgerEntity>)

    /** Full-row update — used by the adjustment edit flow to change qty/remark and stamp audit. */
    @Update
    suspend fun update(entry: StockLedgerEntity)

    /** Load a single ledger row by primary key — needed before updating to read existing audit fields. */
    @Query("SELECT * FROM stock_ledger WHERE id = :id")
    suspend fun findById(id: Long): StockLedgerEntity?

    /**
     * Reactive list of LOSS and ADJUSTMENT entries whose [timestamp] falls within [from]..[to].
     * Used by the date-based Adjustment History screen.
     */
    @Query(
        "SELECT * FROM stock_ledger " +
        "WHERE timestamp >= :from AND timestamp <= :to " +
        "AND change_type IN ('LOSS', 'ADJUSTMENT') " +
        "ORDER BY timestamp DESC, id DESC"
    )
    fun observeAdjustmentsInRange(from: Long, to: Long): Flow<List<StockLedgerEntity>>

    @Query(
        "SELECT IFNULL(SUM(quantity_change), 0) FROM stock_ledger WHERE product_id = :productId"
    )
    fun observeCurrentStock(productId: Long): Flow<Int>

    @Query(
        "SELECT IFNULL(SUM(quantity_change), 0) FROM stock_ledger WHERE product_id = :productId"
    )
    suspend fun getCurrentStock(productId: Long): Int

    @Query(
        "SELECT product_id AS productId, IFNULL(SUM(quantity_change), 0) AS qty " +
            "FROM stock_ledger GROUP BY product_id"
    )
    fun observeAllStock(): Flow<List<ProductStock>>

    @Query(
        "SELECT * FROM stock_ledger WHERE product_id = :productId ORDER BY timestamp DESC, id DESC"
    )
    fun observeForProduct(productId: Long): Flow<List<StockLedgerEntity>>

    @Query(
        "SELECT * FROM stock_ledger WHERE product_id = :productId AND timestamp <= :upTo " +
            "ORDER BY timestamp ASC, id ASC"
    )
    suspend fun entriesUpTo(productId: Long, upTo: Long): List<StockLedgerEntity>
}

data class ProductStock(val productId: Long, val qty: Int)
