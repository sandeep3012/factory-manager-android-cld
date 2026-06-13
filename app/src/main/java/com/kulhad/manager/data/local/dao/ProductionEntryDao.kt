package com.kulhad.manager.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kulhad.manager.data.local.entity.ProductionEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductionEntryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: ProductionEntryEntity): Long

    @Update
    suspend fun update(entry: ProductionEntryEntity)

    @Query("SELECT * FROM production_entries WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): ProductionEntryEntity?

    @Query("SELECT * FROM production_entries ORDER BY date DESC, id DESC")
    fun observeAll(): Flow<List<ProductionEntryEntity>>

    @Query(
        "SELECT * FROM production_entries " +
            "WHERE date BETWEEN :from AND :to ORDER BY date DESC, id DESC"
    )
    fun observeInRange(from: Long, to: Long): Flow<List<ProductionEntryEntity>>

    @Query(
        "SELECT IFNULL(SUM(quantity_produced - defective_quantity), 0) " +
            "FROM production_entries WHERE date BETWEEN :from AND :to"
    )
    fun observeNetQtyInRange(from: Long, to: Long): Flow<Int>

    @Query(
        "SELECT IFNULL(SUM(quantity_produced), 0) " +
            "FROM production_entries WHERE date BETWEEN :from AND :to"
    )
    fun observeTotalQtyInRange(from: Long, to: Long): Flow<Int>

    @Query(
        "SELECT IFNULL(SUM(defective_quantity), 0) " +
            "FROM production_entries WHERE date BETWEEN :from AND :to"
    )
    fun observeDefectiveQtyInRange(from: Long, to: Long): Flow<Int>

    @Query(
        "SELECT IFNULL(SUM((quantity_produced - defective_quantity) * rate_snapshot), 0) " +
            "FROM production_entries WHERE date BETWEEN :from AND :to"
    )
    fun observeLaborCostInRange(from: Long, to: Long): Flow<Double>

    @Query(
        "SELECT IFNULL(SUM((quantity_produced - defective_quantity) * rate_snapshot), 0) " +
            "FROM production_entries WHERE worker_id = :workerId AND date BETWEEN :from AND :to"
    )
    suspend fun earningsForWorkerInRange(workerId: Long, from: Long, to: Long): Double

    /**
     * Reactive per-worker piece earnings for a date range.
     *
     * Returns only workers who have at least one production entry in [from]..[to].
     * Workers absent from the result contributed ₹0 in that period.
     * Used by [ProfitabilityRepository] to build a worker-type-aware labour cost flow
     * without requiring a suspend context.
     */
    @Query("""
        SELECT worker_id AS workerId,
               IFNULL(SUM((quantity_produced - defective_quantity) * rate_snapshot), 0.0) AS earnings
        FROM production_entries
        WHERE date BETWEEN :from AND :to
        GROUP BY worker_id
    """)
    fun observeEarningsByWorkerInRange(from: Long, to: Long): Flow<List<WorkerEarnings>>

    @Query(
        "SELECT IFNULL(SUM(quantity_produced - defective_quantity), 0) " +
            "FROM production_entries WHERE worker_id = :workerId AND date BETWEEN :from AND :to"
    )
    suspend fun netQtyForWorkerInRange(workerId: Long, from: Long, to: Long): Int

    @Query(
        "SELECT product_id AS productId, SUM(quantity_produced - defective_quantity) AS qty " +
            "FROM production_entries WHERE date BETWEEN :from AND :to " +
            "GROUP BY product_id ORDER BY qty DESC"
    )
    fun observeByProductInRange(from: Long, to: Long): Flow<List<ProductQty>>

    @Query(
        "SELECT worker_id AS workerId, SUM(quantity_produced - defective_quantity) AS qty " +
            "FROM production_entries WHERE date BETWEEN :from AND :to " +
            "GROUP BY worker_id ORDER BY qty DESC"
    )
    fun observeByWorkerInRange(from: Long, to: Long): Flow<List<WorkerQty>>

    @Query(
        "SELECT date AS day, SUM(quantity_produced - defective_quantity) AS qty " +
            "FROM production_entries WHERE date BETWEEN :from AND :to " +
            "GROUP BY date ORDER BY date ASC"
    )
    fun observeDailyInRange(from: Long, to: Long): Flow<List<DailyQty>>

    // ── Worker history queries ────────────────────────────────────────────────

    /** Count of production entries ever submitted by a worker. */
    @Query("SELECT COUNT(*) FROM production_entries WHERE worker_id = :workerId")
    suspend fun countEntriesForWorker(workerId: Long): Int

    /** Total net good quantity ever produced by a worker across all time. */
    @Query(
        "SELECT IFNULL(SUM(quantity_produced - defective_quantity), 0) " +
            "FROM production_entries WHERE worker_id = :workerId"
    )
    suspend fun totalNetQtyForWorker(workerId: Long): Int

    /** Total defective quantity ever attributed to a worker. */
    @Query(
        "SELECT IFNULL(SUM(defective_quantity), 0) FROM production_entries WHERE worker_id = :workerId"
    )
    suspend fun totalDefectiveForWorker(workerId: Long): Int

    /** Total piece earnings for a worker across all time. */
    @Query(
        "SELECT IFNULL(SUM((quantity_produced - defective_quantity) * rate_snapshot), 0) " +
            "FROM production_entries WHERE worker_id = :workerId"
    )
    suspend fun totalEarningsForWorker(workerId: Long): Double

    /** Most recent [limit] production entries for a worker, newest first. */
    @Query(
        "SELECT * FROM production_entries WHERE worker_id = :workerId " +
            "ORDER BY date DESC, id DESC LIMIT :limit"
    )
    fun observeRecentForWorker(workerId: Long, limit: Int): Flow<List<ProductionEntryEntity>>
}

data class ProductQty(val productId: Long, val qty: Int)
data class WorkerQty(val workerId: Long, val qty: Int)
data class DailyQty(val day: Long, val qty: Int)
data class WorkerEarnings(val workerId: Long, val earnings: Double)
