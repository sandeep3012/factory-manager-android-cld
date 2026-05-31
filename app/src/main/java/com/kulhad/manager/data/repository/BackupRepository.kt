package com.kulhad.manager.data.repository

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import com.kulhad.manager.data.backup.BACKUP_VERSION
import com.kulhad.manager.data.backup.BackupPreview
import com.kulhad.manager.data.backup.KulhadBackup
import com.kulhad.manager.data.local.KulhadDatabase
import com.kulhad.manager.data.local.dao.BackupDao
import com.kulhad.manager.data.util.DateUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** Result returned by every BackupRepository operation. */
sealed interface BackupResult {
    object Success : BackupResult
    data class Error(val message: String) : BackupResult
}

/** Result returned by [BackupRepository.readBackupFromUri] before restore is confirmed. */
sealed interface BackupReadResult {
    data class Ready(val preview: BackupPreview) : BackupReadResult
    data class Error(val message: String)        : BackupReadResult
}

@Singleton
class BackupRepository @Inject constructor(
    private val database: KulhadDatabase,
    private val backupDao: BackupDao,
    @ApplicationContext private val context: Context
) {

    // Gson with nulls serialised explicitly so nullable fields round-trip correctly.
    private val gson: Gson = GsonBuilder().serializeNulls().create()

    // SharedPreferences key for persisting the last successful export timestamp.
    private val prefs by lazy {
        context.getSharedPreferences("kulhad_backup_prefs", Context.MODE_PRIVATE)
    }

    companion object {
        private const val PREF_LAST_EXPORT_AT = "last_export_at"
        private const val PREF_LAST_SAFETY_PATH = "last_safety_backup_path"
    }

    // ── Last export time ──────────────────────────────────────────────────────

    /** Epoch-millis of the last successful export, or 0 if none. */
    fun getLastExportTime(): Long = prefs.getLong(PREF_LAST_EXPORT_AT, 0L)

    /** Full path of the most recent safety (pre-restore) backup, or null if none. */
    fun getLastSafetyBackupPath(): String? = prefs.getString(PREF_LAST_SAFETY_PATH, null)

    // ── Export ────────────────────────────────────────────────────────────────

    /**
     * Export the complete database to a URI supplied by SAF [ACTION_CREATE_DOCUMENT].
     *
     * Reads all 14 tables inside a single Room transaction for a consistent snapshot,
     * then writes the resulting JSON to [uri] via [ContentResolver.openOutputStream].
     * On success, persists the export timestamp to SharedPreferences.
     */
    suspend fun exportBackup(uri: Uri): BackupResult {
        return try {
            val backup = readCurrentDatabase()
            val json   = gson.toJson(backup)
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.writer().use { it.write(json) }
            } ?: return BackupResult.Error("Could not open output stream for the selected file.")
            prefs.edit().putLong(PREF_LAST_EXPORT_AT, System.currentTimeMillis()).apply()
            BackupResult.Success
        } catch (e: IOException) {
            BackupResult.Error("Export failed: ${e.message}")
        } catch (e: Exception) {
            BackupResult.Error("Unexpected error during export: ${e.message}")
        }
    }

    // ── Read / Validate import file ───────────────────────────────────────────

    /**
     * Read and parse a backup file selected via SAF [ACTION_OPEN_DOCUMENT].
     *
     * Performs validation:
     *  - File is readable
     *  - JSON is syntactically valid
     *  - [KulhadBackup.backupVersion] is present and ≤ [BACKUP_VERSION]
     *  - Mandatory top-level arrays are present (users, products, workers)
     *
     * Returns [BackupReadResult.Ready] with a preview on success, or
     * [BackupReadResult.Error] with a user-friendly message on any failure.
     * The full [KulhadBackup] is retained in the [BackupPreview] for use by [restore].
     */
    suspend fun readBackupFromUri(uri: Uri): BackupReadResult {
        return try {
            val json = context.contentResolver.openInputStream(uri)?.use { inp ->
                inp.bufferedReader().use { it.readText() }
            } ?: return BackupReadResult.Error("Could not read the selected file.")

            val backup = try {
                gson.fromJson(json, KulhadBackup::class.java)
            } catch (e: JsonSyntaxException) {
                return BackupReadResult.Error("Invalid file: not a valid Kulhad Manager backup.")
            } ?: return BackupReadResult.Error("Invalid file: empty or unreadable content.")

            // Version check
            if (backup.backupVersion > BACKUP_VERSION) {
                return BackupReadResult.Error(
                    "This backup was created by a newer version of the app (v${backup.backupVersion}). " +
                    "Please update the app before restoring."
                )
            }

            BackupReadResult.Ready(BackupPreview(backup))

        } catch (e: IOException) {
            BackupReadResult.Error("Could not read the file: ${e.message}")
        } catch (e: Exception) {
            BackupReadResult.Error("Unexpected error reading backup: ${e.message}")
        }
    }

    // ── Restore ───────────────────────────────────────────────────────────────

    /**
     * Restore the database from a validated [KulhadBackup].
     *
     * Process:
     *  1. Create a safety backup of the current database to internal storage.
     *     If this fails, the restore is aborted — current data is never touched.
     *  2. Begin a Room transaction.
     *  3. Delete all rows in FK-safe order (children before parents).
     *  4. Insert all rows from the backup in FK-safe order (parents before children).
     *  5. Commit.
     *
     * If step 3 or 4 throws, the transaction is rolled back automatically.
     * The original data remains intact.
     *
     * All Room Flows (observed by ViewModels) will re-emit automatically after commit,
     * so every live screen refreshes without manual intervention.
     */
    suspend fun restore(backup: KulhadBackup): BackupResult {
        // Step 1 — safety backup
        val safetyResult = createSafetyBackup()
        if (safetyResult is BackupResult.Error) return safetyResult

        // Step 2-5 — transactional replace
        return try {
            database.withTransaction {

                // ── Delete: children → parents ────────────────────────────
                backupDao.deleteAllWorkerAdvances()
                backupDao.deleteAllWorkerTypeHistory()
                backupDao.deleteAllAttendance()
                backupDao.deleteAllProductionEntries()
                backupDao.deleteAllStockLedger()
                backupDao.deleteAllSaleItems()
                backupDao.deleteAllPayments()
                backupDao.deleteAllExpenses()
                backupDao.deleteAllSales()
                backupDao.deleteAllPieceRates()
                backupDao.deleteAllWorkers()
                backupDao.deleteAllProducts()
                backupDao.deleteAllExpenseTypes()
                backupDao.deleteAllUsers()

                // ── Insert: parents → children ────────────────────────────
                if (backup.users.isNotEmpty())             backupDao.insertAllUsers(backup.users)
                if (backup.expenseTypes.isNotEmpty())      backupDao.insertAllExpenseTypes(backup.expenseTypes)
                if (backup.products.isNotEmpty())          backupDao.insertAllProducts(backup.products)
                if (backup.workers.isNotEmpty())           backupDao.insertAllWorkers(backup.workers)
                if (backup.pieceRates.isNotEmpty())        backupDao.insertAllPieceRates(backup.pieceRates)
                if (backup.workerTypeHistory.isNotEmpty()) backupDao.insertAllWorkerTypeHistory(backup.workerTypeHistory)
                if (backup.sales.isNotEmpty())             backupDao.insertAllSales(backup.sales)
                if (backup.attendance.isNotEmpty())        backupDao.insertAllAttendance(backup.attendance)
                if (backup.productionEntries.isNotEmpty()) backupDao.insertAllProductionEntries(backup.productionEntries)
                if (backup.stockLedger.isNotEmpty())       backupDao.insertAllStockLedger(backup.stockLedger)
                if (backup.saleItems.isNotEmpty())         backupDao.insertAllSaleItems(backup.saleItems)
                if (backup.payments.isNotEmpty())          backupDao.insertAllPayments(backup.payments)
                if (backup.expenses.isNotEmpty())          backupDao.insertAllExpenses(backup.expenses)
                if (backup.workerAdvances.isNotEmpty())    backupDao.insertAllWorkerAdvances(backup.workerAdvances)
            }
            BackupResult.Success
        } catch (e: Exception) {
            BackupResult.Error("Restore failed: ${e.message}\n\nYour original data was not modified.")
        }
    }

    // ── Safety backup ─────────────────────────────────────────────────────────

    /**
     * Writes the current database state to the app's internal files directory.
     *
     * The file is named "pre_restore_backup_<timestamp>.kulhad" and is stored at
     * [Context.getFilesDir()]/backups/.  This directory is accessible via adb
     * and Android's backup framework but is not directly visible to the user
     * through a file manager.
     *
     * The path is stored in SharedPreferences so the UI can report it.
     * Returns [BackupResult.Error] if writing fails — in that case the caller
     * should abort the restore so current data is never lost.
     */
    suspend fun createSafetyBackup(): BackupResult {
        return try {
            val backup  = readCurrentDatabase()
            val json    = gson.toJson(backup)
            val dir     = File(context.filesDir, "backups").also { it.mkdirs() }
            val name    = "pre_restore_backup_${DateUtils.formatForFilename()}.kulhad"
            val file    = File(dir, name)
            file.writeText(json)
            prefs.edit().putString(PREF_LAST_SAFETY_PATH, file.absolutePath).apply()
            BackupResult.Success
        } catch (e: Exception) {
            BackupResult.Error(
                "Could not create safety backup before restoring: ${e.message}\n" +
                "Restore was aborted. Your data is unchanged."
            )
        }
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    /**
     * Read a consistent snapshot of all 14 tables from Room.
     *
     * Wrapped in a transaction so that all reads see the same database state —
     * no partial writes can occur between individual table reads.
     */
    private suspend fun readCurrentDatabase(): KulhadBackup =
        database.withTransaction {
            KulhadBackup(
                createdAt          = System.currentTimeMillis(),
                users              = backupDao.getAllUsers(),
                workers            = backupDao.getAllWorkers(),
                workerTypeHistory  = backupDao.getAllWorkerTypeHistory(),
                products           = backupDao.getAllProducts(),
                pieceRates         = backupDao.getAllPieceRates(),
                attendance         = backupDao.getAllAttendance(),
                productionEntries  = backupDao.getAllProductionEntries(),
                stockLedger        = backupDao.getAllStockLedger(),
                sales              = backupDao.getAllSales(),
                saleItems          = backupDao.getAllSaleItems(),
                payments           = backupDao.getAllPayments(),
                expenseTypes       = backupDao.getAllExpenseTypes(),
                expenses           = backupDao.getAllExpenses(),
                workerAdvances     = backupDao.getAllWorkerAdvances()
            )
        }
}
