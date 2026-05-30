package com.kulhad.manager.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kulhad.manager.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(products: List<ProductEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(product: ProductEntity): Long

    /** Full-row update — used by Product Master edit flow; stamps audit_updated_by/At. */
    @Update
    suspend fun update(product: ProductEntity)

    /**
     * Active products ordered by display_order ASC, then size_ml ASC.
     * Used by all entry screens (production, sales, stock adjustment).
     * The secondary sort on size_ml preserves a sensible order for rows where
     * display_order is 0 (migrated rows before the user sets an explicit order).
     */
    @Query("SELECT * FROM products WHERE is_active = 1 ORDER BY display_order ASC, size_ml ASC")
    fun observeActive(): Flow<List<ProductEntity>>

    /**
     * ALL products (active and inactive), same ordering.
     * Used by Product Master screen to show the full list including inactive entries.
     */
    @Query("SELECT * FROM products ORDER BY display_order ASC, size_ml ASC")
    fun observeAll(): Flow<List<ProductEntity>>

    /** One-shot read of all products — used by report queries that don't need reactivity. */
    @Query("SELECT * FROM products ORDER BY display_order ASC, size_ml ASC")
    suspend fun getAll(): List<ProductEntity>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): ProductEntity?

    /** Check for duplicate display_label (case-insensitive), excluding [excludeId] for edit flows. */
    @Query("SELECT COUNT(*) FROM products WHERE LOWER(display_label) = LOWER(:label) AND id != :excludeId")
    suspend fun countByLabel(label: String, excludeId: Long = 0): Int
}
