package com.kulhad.manager.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kulhad.manager.data.local.entity.StockLedgerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StockLedgerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: StockLedgerEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<StockLedgerEntity>)

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
