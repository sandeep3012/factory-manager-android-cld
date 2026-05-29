package com.kulhad.manager.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Central registry of all Room database schema migrations for Kulhad Manager.
 *
 * ─── Current state ──────────────────────────────────────────────────────────────
 * Version: 2
 *
 * ─── v1 schema — 14 tables ──────────────────────────────────────────────────────
 *
 *  users              id, name, email, password_hash, created_at
 *  workers            id, name, phone, address, joining_date, current_type,
 *                       daily_rate, is_active
 *  worker_type_history  id, worker_id, worker_type, daily_rate, effective_from
 *  products           id, size_ml, description, is_active
 *  piece_rates        id, product_id, rate_per_piece, effective_from
 *  attendance         id, worker_id, date, is_present
 *  production_entries id, worker_id, product_id, quantity_produced,
 *                       defective_quantity, rate_snapshot, date, created_by, created_at
 *  stock_ledger       id, product_id, quantity_change, change_type, remark,
 *                       done_by, timestamp
 *  sales              id, customer_name, date, total_amount, created_by
 *  sale_items         id, sale_id, product_id, quantity, price_per_unit
 *  payments           id, sale_id, amount, date, remark
 *  expense_types      id, name, is_active
 *  expenses           id, expense_type_id, amount, date, remark, added_by
 *  worker_advances    id, worker_id, amount, date, remark
 *
 * ─── v2 additions (MIGRATION_1_2) ───────────────────────────────────────────────
 *
 *  attendance, worker_advances, payments, production_entries, sales,
 *  worker_type_history, stock_ledger each gain:
 *    audit_created_by  TEXT NOT NULL DEFAULT 'System'
 *    audit_created_at  INTEGER NOT NULL DEFAULT 0
 *    audit_updated_by  TEXT (nullable)
 *    audit_updated_at  INTEGER (nullable)
 *
 * ─── How to add a migration ─────────────────────────────────────────────────────
 *
 * Whenever you change ANY @Entity (add/rename/drop a column, add a new table,
 * change a column default, etc.) you MUST follow these steps to prevent data loss:
 *
 *  Step 1 — Bump the version in KulhadDatabase.kt
 *    @Database(version = 2, ...)     ← was 1
 *
 *  Step 2 — Write the migration SQL below (name: MIGRATION_X_Y)
 *    val MIGRATION_1_2 = object : Migration(1, 2) {
 *        override fun migrate(db: SupportSQLiteDatabase) {
 *            // SQL that transforms v1 schema rows into v2 schema rows.
 *            // Example — add a nullable TEXT column:
 *            db.execSQL("ALTER TABLE workers ADD COLUMN nickname TEXT DEFAULT NULL")
 *        }
 *    }
 *
 *  Step 3 — Register it in ALL (keep in version order)
 *    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2)
 *
 *  Step 4 — Build to verify
 *    ./gradlew assembleDebug
 *    Room compares 1.json + your SQL against the new 2.json baseline.
 *    If the schemas don't match, the build fails with a clear diff — fix the SQL.
 *
 * ─── Common migration patterns ──────────────────────────────────────────────────
 *
 *  ADD a nullable column:
 *    db.execSQL("ALTER TABLE workers ADD COLUMN nickname TEXT")
 *
 *  ADD a NOT NULL column (existing rows get the default value):
 *    db.execSQL("ALTER TABLE expenses ADD COLUMN category TEXT NOT NULL DEFAULT 'Other'")
 *
 *  ADD a new table (copy the CREATE TABLE from the generated schema JSON):
 *    db.execSQL("""
 *        CREATE TABLE IF NOT EXISTS `tags` (
 *            `id` INTEGER PRIMARY KEY NOT NULL,
 *            `name` TEXT NOT NULL
 *        )
 *    """)
 *
 *  DROP a column / RENAME a column (SQLite < API 29 doesn't support these natively):
 *    Use the copy-table pattern:
 *      1. CREATE TABLE new_name AS desired schema
 *      2. INSERT INTO new_name SELECT col1, col2, ... FROM old_name
 *      3. DROP TABLE old_name
 *      4. ALTER TABLE new_name RENAME TO old_name
 *      5. Re-create any indexes that existed on the old table
 *
 *  Multi-version upgrade (user skips versions, e.g. 1 → 3):
 *    Room applies migrations in sequence automatically: MIGRATION_1_2 then MIGRATION_2_3.
 *    You do NOT need a MIGRATION_1_3 unless you want to optimise the path.
 */
object Migrations {

    /**
     * v1 → v2: Add audit columns to 7 tables.
     *
     * Four columns are appended to each table:
     *   audit_created_by  TEXT NOT NULL DEFAULT 'System'
     *   audit_created_at  INTEGER NOT NULL DEFAULT 0
     *   audit_updated_by  TEXT (nullable, no default)
     *   audit_updated_at  INTEGER (nullable, no default)
     *
     * Column names use the "audit_" prefix on every table so that:
     *  - production_entries and sales (which already have integer `created_by` FK columns)
     *    have zero naming conflicts.
     *  - All 7 tables share an identical, predictable naming scheme.
     *
     * Existing rows receive 'System'/0/NULL/NULL for the new columns — safe defaults
     * that indicate "created before audit tracking was introduced."
     *
     * Tables affected:
     *   attendance, worker_advances, payments, production_entries,
     *   sales, worker_type_history, stock_ledger
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            val tables = listOf(
                "attendance",
                "worker_advances",
                "payments",
                "production_entries",
                "sales",
                "worker_type_history",
                "stock_ledger"
            )
            for (table in tables) {
                db.execSQL("ALTER TABLE `$table` ADD COLUMN `audit_created_by` TEXT NOT NULL DEFAULT 'System'")
                db.execSQL("ALTER TABLE `$table` ADD COLUMN `audit_created_at` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `$table` ADD COLUMN `audit_updated_by` TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE `$table` ADD COLUMN `audit_updated_at` INTEGER DEFAULT NULL")
            }
        }
    }

    /**
     * All migrations in ascending version order.
     * Room applies them sequentially — add new ones at the end of the array.
     */
    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2)
}
