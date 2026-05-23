package com.kulhad.manager.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kulhad.manager.data.local.entity.WorkerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(worker: WorkerEntity): Long

    @Update
    suspend fun update(worker: WorkerEntity)

    @Query("SELECT * FROM workers WHERE is_active = 1 ORDER BY name ASC")
    fun observeActive(): Flow<List<WorkerEntity>>

    @Query("SELECT * FROM workers ORDER BY name ASC")
    fun observeAll(): Flow<List<WorkerEntity>>

    @Query("SELECT * FROM workers WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): WorkerEntity?

    @Query("SELECT * FROM workers WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<WorkerEntity?>

    @Query("SELECT COUNT(*) FROM workers WHERE is_active = 1")
    fun observeActiveCount(): Flow<Int>

    @Query(
        "UPDATE workers SET current_type = :type, daily_rate = :dailyRate WHERE id = :id"
    )
    suspend fun updateTypeAndRate(id: Long, type: String, dailyRate: Int)

    @Query("UPDATE workers SET is_active = :active WHERE id = :id")
    suspend fun setActive(id: Long, active: Boolean)

    /**
     * Updates only profile fields (name, phone, address, joining_date, is_active).
     * current_type and daily_rate are intentionally excluded — callers MUST use
     * [updateTypeAndRate] (inside a transaction that also inserts a history row)
     * to change worker type or rate. This makes it structurally impossible for
     * a plain profile edit to bypass history tracking.
     */
    @Query("""
        UPDATE workers
        SET name = :name, phone = :phone, address = :address,
            joining_date = :joiningDate, is_active = :isActive
        WHERE id = :id
    """)
    suspend fun updateProfile(
        id: Long,
        name: String,
        phone: String,
        address: String,
        joiningDate: Long,
        isActive: Boolean
    )
}
