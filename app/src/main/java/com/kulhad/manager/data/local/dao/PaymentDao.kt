package com.kulhad.manager.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kulhad.manager.data.local.entity.PaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(payment: PaymentEntity): Long

    @Query("SELECT * FROM payments WHERE sale_id = :saleId ORDER BY date DESC, id DESC")
    fun observeForSale(saleId: Long): Flow<List<PaymentEntity>>

    @Query("SELECT IFNULL(SUM(amount), 0) FROM payments WHERE sale_id = :saleId")
    suspend fun paidForSale(saleId: Long): Int

    @Query("SELECT IFNULL(SUM(amount), 0) FROM payments WHERE sale_id = :saleId")
    fun observePaidForSale(saleId: Long): Flow<Int>

    @Query(
        "SELECT IFNULL(SUM(amount), 0) FROM payments WHERE date BETWEEN :from AND :to"
    )
    fun observeCollectedInRange(from: Long, to: Long): Flow<Int>

    @Query(
        "SELECT sale_id AS saleId, IFNULL(SUM(amount), 0) AS paid " +
            "FROM payments GROUP BY sale_id"
    )
    fun observeAllSalePaid(): Flow<List<SalePaid>>
}

data class SalePaid(val saleId: Long, val paid: Int)
