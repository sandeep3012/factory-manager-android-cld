package com.kulhad.manager.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kulhad.manager.data.local.entity.PieceRateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PieceRateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rate: PieceRateEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(rates: List<PieceRateEntity>)

    @Query(
        "SELECT * FROM piece_rates WHERE product_id = :productId " +
            "ORDER BY effective_from DESC LIMIT 1"
    )
    suspend fun currentRate(productId: Long): PieceRateEntity?

    @Query(
        "SELECT * FROM piece_rates WHERE product_id = :productId " +
            "ORDER BY effective_from DESC LIMIT 1"
    )
    fun observeCurrentRate(productId: Long): Flow<PieceRateEntity?>

    @Query(
        "SELECT * FROM piece_rates WHERE product_id = :productId " +
            "ORDER BY effective_from DESC"
    )
    fun observeHistory(productId: Long): Flow<List<PieceRateEntity>>
}
