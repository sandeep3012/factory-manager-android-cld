package com.kulhad.manager.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kulhad.manager.data.local.entity.ExpenseTypeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseTypeDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(type: ExpenseTypeEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(types: List<ExpenseTypeEntity>)

    @Query("SELECT * FROM expense_types WHERE is_active = 1 ORDER BY id ASC")
    fun observeActive(): Flow<List<ExpenseTypeEntity>>

    @Query("SELECT * FROM expense_types WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): ExpenseTypeEntity?

    @Query("SELECT * FROM expense_types WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): ExpenseTypeEntity?
}
