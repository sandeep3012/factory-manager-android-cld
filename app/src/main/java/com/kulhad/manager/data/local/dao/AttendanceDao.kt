package com.kulhad.manager.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kulhad.manager.data.local.entity.AttendanceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AttendanceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<AttendanceEntity>)

    @Query(
        "SELECT * FROM attendance WHERE date = :day"
    )
    fun observeByDate(day: Long): Flow<List<AttendanceEntity>>

    @Query(
        "SELECT * FROM attendance WHERE worker_id = :workerId AND date = :day LIMIT 1"
    )
    suspend fun findByWorkerAndDate(workerId: Long, day: Long): AttendanceEntity?

    @Query(
        "SELECT COUNT(*) FROM attendance WHERE date = :day AND is_present = 1"
    )
    fun observePresentCount(day: Long): Flow<Int>

    @Query(
        "SELECT COUNT(*) FROM attendance WHERE date = :day AND is_present = 0"
    )
    fun observeAbsentCount(day: Long): Flow<Int>

    @Query(
        "SELECT date AS day, SUM(CASE WHEN is_present = 1 THEN 1 ELSE 0 END) AS presentCount " +
            "FROM attendance WHERE date BETWEEN :from AND :to GROUP BY date ORDER BY date ASC"
    )
    fun observeRangeCounts(from: Long, to: Long): Flow<List<DailyAttendanceCount>>

    @Query(
        "SELECT COUNT(*) FROM attendance WHERE worker_id = :workerId " +
            "AND date BETWEEN :from AND :to AND is_present = 1"
    )
    suspend fun countPresentInRange(workerId: Long, from: Long, to: Long): Int

    /**
     * Returns all attendance rows for a given [date], optionally filtered to a single worker.
     *
     * When [workerId] is null, the `:workerId IS NULL` clause evaluates to TRUE and all
     * rows for that date are returned. Room 2.6+ binds Kotlin `Long?` as SQL NULL correctly.
     */
    @Query("""
        SELECT * FROM attendance
        WHERE date = :date
        AND (:workerId IS NULL OR worker_id = :workerId)
        ORDER BY worker_id
    """)
    fun observeAttendanceHistory(date: Long, workerId: Long?): Flow<List<AttendanceEntity>>

    /**
     * Updates the [isPresent] flag on an existing attendance row.
     *
     * Uses a targeted UPDATE — never inserts — so duplicate rows are structurally impossible.
     * If no matching row exists, the UPDATE is a no-op (zero rows affected).
     */
    @Query("""
        UPDATE attendance
        SET is_present = :isPresent
        WHERE worker_id = :workerId
        AND date = :date
    """)
    suspend fun updateAttendance(workerId: Long, date: Long, isPresent: Boolean)
}

data class DailyAttendanceCount(
    val day: Long,
    val presentCount: Int
)
