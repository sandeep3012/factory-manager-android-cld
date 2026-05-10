package com.kulhad.manager.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kulhad.manager.data.local.entity.WorkerTypeHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkerTypeHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: WorkerTypeHistoryEntity): Long

    @Query(
        "SELECT * FROM worker_type_history WHERE worker_id = :workerId " +
            "ORDER BY effective_from DESC"
    )
    fun observeForWorker(workerId: Long): Flow<List<WorkerTypeHistoryEntity>>

    @Query(
        "SELECT * FROM worker_type_history WHERE worker_id = :workerId " +
            "ORDER BY effective_from DESC LIMIT 1"
    )
    suspend fun latestForWorker(workerId: Long): WorkerTypeHistoryEntity?

    @Query(
        "SELECT * FROM worker_type_history WHERE worker_id = :workerId " +
            "AND effective_from <= :at ORDER BY effective_from DESC LIMIT 1"
    )
    suspend fun typeAt(workerId: Long, at: Long): WorkerTypeHistoryEntity?
}
