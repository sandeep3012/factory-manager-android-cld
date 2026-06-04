package com.kulhad.manager.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kulhad.manager.data.local.entity.PieceRateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PieceRateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rate: PieceRateEntity): Long

    /** Full-row update — used by the rate edit flow to stamp audit_updated_by/at. */
    @Update
    suspend fun update(rate: PieceRateEntity)

    /** One-shot read of a single rate row by primary key. */
    @Query("SELECT * FROM piece_rates WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): PieceRateEntity?

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

    /**
     * Observe the entire piece_rates table.
     * Used as a reactive trigger in [ProductMasterViewModel] — whenever any rate row is
     * inserted, this Flow re-emits and forces the combined product+rate Flow to reload
     * fresh rate values for every product.
     */
    @Query("SELECT * FROM piece_rates")
    fun observeAllRates(): Flow<List<PieceRateEntity>>
}
