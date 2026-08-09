package com.kulhad.manager.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kulhad.manager.data.local.entity.WorkerAdvanceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkerAdvanceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: WorkerAdvanceEntity): Long

    @Query("SELECT * FROM worker_advances WHERE worker_id = :workerId ORDER BY date DESC, id DESC")
    fun observeForWorker(workerId: Long): Flow<List<WorkerAdvanceEntity>>

    @Query(
        "SELECT * FROM worker_advances WHERE worker_id = :workerId AND date BETWEEN :from AND :to " +
            "ORDER BY date DESC, id DESC"
    )
    fun observeForWorkerInRange(workerId: Long, from: Long, to: Long): Flow<List<WorkerAdvanceEntity>>

    @Query("SELECT * FROM worker_advances ORDER BY date DESC, id DESC")
    fun observeAll(): Flow<List<WorkerAdvanceEntity>>

    @Query(
        "SELECT IFNULL(SUM(amount), 0) FROM worker_advances " +
            "WHERE worker_id = :workerId AND date BETWEEN :from AND :to"
    )
    suspend fun totalForWorkerInRange(workerId: Long, from: Long, to: Long): Int

    @Query(
        "SELECT IFNULL(SUM(amount), 0) FROM worker_advances WHERE date BETWEEN :from AND :to"
    )
    fun observeTotalInRange(from: Long, to: Long): Flow<Int>

    @Query(
        "SELECT IFNULL(SUM(amount), 0) FROM worker_advances " +
            "WHERE worker_id = :workerId AND date BETWEEN :from AND :to"
    )
    fun observeTotalForWorkerInRange(workerId: Long, from: Long, to: Long): Flow<Int>

    /** Total advances given to a worker across all time. */
    @Query("SELECT IFNULL(SUM(amount), 0) FROM worker_advances WHERE worker_id = :workerId")
    suspend fun totalForWorker(workerId: Long): Int

    /** Most recent [limit] advance rows for a worker, newest first. */
    @Query(
        "SELECT * FROM worker_advances WHERE worker_id = :workerId " +
            "ORDER BY date DESC, id DESC LIMIT :limit"
    )
    fun observeRecentForWorker(workerId: Long, limit: Int): Flow<List<WorkerAdvanceEntity>>
}
