package com.kulhad.manager.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kulhad.manager.data.local.entity.SaleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sale: SaleEntity): Long

    @Query("SELECT * FROM sales ORDER BY date DESC, id DESC")
    fun observeAll(): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sales WHERE date BETWEEN :from AND :to ORDER BY date DESC, id DESC")
    fun observeInRange(from: Long, to: Long): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sales WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): SaleEntity?

    @Query("SELECT * FROM sales WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<SaleEntity?>

    @Query("SELECT IFNULL(SUM(total_amount), 0) FROM sales WHERE date BETWEEN :from AND :to")
    fun observeTotalInRange(from: Long, to: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM sales WHERE date BETWEEN :from AND :to")
    fun observeOrderCountInRange(from: Long, to: Long): Flow<Int>

    @Query(
        "SELECT date AS day, IFNULL(SUM(total_amount), 0) AS amount " +
            "FROM sales WHERE date BETWEEN :from AND :to GROUP BY date ORDER BY date ASC"
    )
    fun observeDailySales(from: Long, to: Long): Flow<List<DailySalesAmount>>

    @Query(
        "SELECT customer_name AS customer, IFNULL(SUM(total_amount), 0) AS amount " +
            "FROM sales WHERE date BETWEEN :from AND :to GROUP BY customer_name ORDER BY amount DESC"
    )
    fun observeCustomerTotals(from: Long, to: Long): Flow<List<CustomerTotal>>
}

data class DailySalesAmount(val day: Long, val amount: Int)
data class CustomerTotal(val customer: String, val amount: Int)
