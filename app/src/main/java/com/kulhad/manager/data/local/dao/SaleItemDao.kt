package com.kulhad.manager.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kulhad.manager.data.local.entity.SaleItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleItemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SaleItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<SaleItemEntity>)

    @Query("SELECT * FROM sale_items WHERE sale_id = :saleId ORDER BY id ASC")
    suspend fun forSale(saleId: Long): List<SaleItemEntity>

    @Query("SELECT * FROM sale_items WHERE sale_id = :saleId ORDER BY id ASC")
    fun observeForSale(saleId: Long): Flow<List<SaleItemEntity>>
}
