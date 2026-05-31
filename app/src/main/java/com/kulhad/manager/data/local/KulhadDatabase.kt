package com.kulhad.manager.data.local

import android.content.ContentValues
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.kulhad.manager.data.local.dao.AttendanceDao
import com.kulhad.manager.data.local.dao.BackupDao
import com.kulhad.manager.data.local.dao.ExpenseDao
import com.kulhad.manager.data.local.dao.ExpenseTypeDao
import com.kulhad.manager.data.local.dao.PaymentDao
import com.kulhad.manager.data.local.dao.PieceRateDao
import com.kulhad.manager.data.local.dao.ProductDao
import com.kulhad.manager.data.local.dao.ProductionEntryDao
import com.kulhad.manager.data.local.dao.SaleDao
import com.kulhad.manager.data.local.dao.SaleItemDao
import com.kulhad.manager.data.local.dao.StockLedgerDao
import com.kulhad.manager.data.local.dao.UserDao
import com.kulhad.manager.data.local.dao.WorkerAdvanceDao
import com.kulhad.manager.data.local.dao.WorkerDao
import com.kulhad.manager.data.local.dao.WorkerTypeHistoryDao
import com.kulhad.manager.data.local.entity.AttendanceEntity
import com.kulhad.manager.data.local.entity.ExpenseEntity
import com.kulhad.manager.data.local.entity.ExpenseTypeEntity
import com.kulhad.manager.data.local.entity.PaymentEntity
import com.kulhad.manager.data.local.entity.PieceRateEntity
import com.kulhad.manager.data.local.entity.ProductEntity
import com.kulhad.manager.data.local.entity.ProductionEntryEntity
import com.kulhad.manager.data.local.entity.SaleEntity
import com.kulhad.manager.data.local.entity.SaleItemEntity
import com.kulhad.manager.data.local.entity.StockLedgerEntity
import com.kulhad.manager.data.local.entity.UserEntity
import com.kulhad.manager.data.local.entity.WorkerAdvanceEntity
import com.kulhad.manager.data.local.entity.WorkerEntity
import com.kulhad.manager.data.local.entity.WorkerTypeHistoryEntity
import com.kulhad.manager.data.local.migration.Migrations
import com.kulhad.manager.data.util.PasswordHasher

@Database(
    entities = [
        UserEntity::class,
        WorkerEntity::class,
        WorkerTypeHistoryEntity::class,
        ProductEntity::class,
        PieceRateEntity::class,
        AttendanceEntity::class,
        ProductionEntryEntity::class,
        StockLedgerEntity::class,
        SaleEntity::class,
        SaleItemEntity::class,
        PaymentEntity::class,
        ExpenseTypeEntity::class,
        ExpenseEntity::class,
        WorkerAdvanceEntity::class
    ],
    version = 4,
    exportSchema = true  // writes app/schemas/…/<version>.json — commit to git
)
@TypeConverters(Converters::class)
abstract class KulhadDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun workerDao(): WorkerDao
    abstract fun backupDao(): BackupDao
    abstract fun workerTypeHistoryDao(): WorkerTypeHistoryDao
    abstract fun productDao(): ProductDao
    abstract fun pieceRateDao(): PieceRateDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun productionEntryDao(): ProductionEntryDao
    abstract fun stockLedgerDao(): StockLedgerDao
    abstract fun saleDao(): SaleDao
    abstract fun saleItemDao(): SaleItemDao
    abstract fun paymentDao(): PaymentDao
    abstract fun expenseTypeDao(): ExpenseTypeDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun workerAdvanceDao(): WorkerAdvanceDao

    companion object {

        const val DB_NAME = "kulhad.db"
        const val DEMO_EMAIL = "owner@kulhad.com"
        const val DEMO_PASSWORD = "kulhad123"
        const val DEMO_NAME = "Owner"

        @Volatile private var INSTANCE: KulhadDatabase? = null

        fun get(context: Context): KulhadDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: build(context).also { INSTANCE = it }
        }

        private fun build(context: Context): KulhadDatabase = Room.databaseBuilder(
            context.applicationContext,
            KulhadDatabase::class.java,
            DB_NAME
        )
            .addCallback(SeedCallback)
            // Safe migration: explicit migrations only.
            // If the schema version is bumped without a matching Migration object,
            // Room throws IllegalStateException at startup — a deliberate crash that
            // prevents silent data loss. Add new migrations to Migrations.ALL first.
            .addMigrations(*Migrations.ALL)
            .build()
    }

    /**
     * Seeds initial data on first creation:
     *   - 8 products (60..250ml)
     *   - 8 piece_rates @ ₹1.20 each
     *   - 3 expense types (Labor, Soil, Transport)
     *   - 1 demo user (owner@kulhad.com / kulhad123)
     */
    private object SeedCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            val now = System.currentTimeMillis()

            // Products — display_order = size_ml keeps the same ascending sort on fresh installs.
            val sizes = intArrayOf(60, 70, 80, 90, 100, 120, 200, 250)
            for (size in sizes) {
                db.insert(
                    "products", android.database.sqlite.SQLiteDatabase.CONFLICT_IGNORE,
                    ContentValues().apply {
                        put("size_ml", size)
                        put("description", "${size}ml Kulhad")
                        put("is_active", 1)
                        put("display_label", "${size}ml")
                        put("display_order", size)
                        put("audit_created_by", "System")
                        put("audit_created_at", now)
                    }
                )
            }

            // Piece rates (1 per product, default ₹1.20)
            // Product IDs are auto-incremented 1..8 in insertion order.
            for (productId in 1..sizes.size) {
                db.insert(
                    "piece_rates", android.database.sqlite.SQLiteDatabase.CONFLICT_IGNORE,
                    ContentValues().apply {
                        put("product_id", productId.toLong())
                        put("rate_per_piece", 1.20)
                        put("effective_from", now)
                    }
                )
            }

            // Expense types
            for (name in listOf("Labor", "Soil", "Transport")) {
                db.insert(
                    "expense_types", android.database.sqlite.SQLiteDatabase.CONFLICT_IGNORE,
                    ContentValues().apply {
                        put("name", name)
                        put("is_active", 1)
                    }
                )
            }

            // Demo user
            db.insert(
                "users", android.database.sqlite.SQLiteDatabase.CONFLICT_IGNORE,
                ContentValues().apply {
                    put("name", DEMO_NAME)
                    put("email", DEMO_EMAIL)
                    put("password_hash", PasswordHasher.sha256(DEMO_PASSWORD))
                    put("created_at", now)
                }
            )
        }
    }
}
