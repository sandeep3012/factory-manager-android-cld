package com.kulhad.manager.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kulhad.manager.data.local.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: ExpenseEntity): Long

    /** Full-row update — used by the expense edit flow to change type/amount/remark and stamp audit. */
    @Update
    suspend fun update(expense: ExpenseEntity)

    /** Load a single expense row by primary key — needed before updating to read existing audit fields. */
    @Query("SELECT * FROM expenses WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): ExpenseEntity?

    @Query("SELECT * FROM expenses ORDER BY date DESC, id DESC")
    fun observeAll(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE date BETWEEN :from AND :to ORDER BY date DESC, id DESC")
    fun observeInRange(from: Long, to: Long): Flow<List<ExpenseEntity>>

    @Query("SELECT IFNULL(SUM(amount), 0) FROM expenses WHERE date BETWEEN :from AND :to")
    fun observeTotalInRange(from: Long, to: Long): Flow<Int>

    @Query(
        "SELECT IFNULL(SUM(amount), 0) FROM expenses " +
            "WHERE expense_type_id = :typeId AND date BETWEEN :from AND :to"
    )
    fun observeTotalByTypeInRange(typeId: Long, from: Long, to: Long): Flow<Int>

    @Query(
        "SELECT expense_type_id AS typeId, IFNULL(SUM(amount), 0) AS amount " +
            "FROM expenses WHERE date BETWEEN :from AND :to " +
            "GROUP BY expense_type_id ORDER BY amount DESC"
    )
    fun observeBreakdownInRange(from: Long, to: Long): Flow<List<ExpenseTypeTotal>>
}

data class ExpenseTypeTotal(val typeId: Long, val amount: Int)
