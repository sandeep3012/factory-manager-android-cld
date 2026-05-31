package com.kulhad.manager.ui.screens.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kulhad.manager.data.backup.BackupPreview
import com.kulhad.manager.data.repository.BackupReadResult
import com.kulhad.manager.data.repository.BackupRepository
import com.kulhad.manager.data.repository.BackupResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ── UI state ──────────────────────────────────────────────────────────────────

sealed interface SettingsUiState {
    /** Default — nothing happening. */
    object Idle : SettingsUiState
    /** Export or safety-backup write in progress. */
    object Exporting : SettingsUiState
    /** File selected for import; reading + parsing in progress. */
    object ImportParsing : SettingsUiState
    /** Parsing complete — waiting for user confirmation. */
    data class ImportReady(val preview: BackupPreview) : SettingsUiState
    /** Restore transaction in progress. */
    object Restoring : SettingsUiState
    /** Operation completed (export or restore). Message is shown as a snackbar/banner. */
    data class Done(val message: String) : SettingsUiState
    /** Any non-recoverable error. Message is shown as an error dialog. */
    data class Error(val message: String) : SettingsUiState
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val backupRepository: BackupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SettingsUiState>(SettingsUiState.Idle)
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    /** Epoch-millis of the last successful export (0 = never). */
    val lastExportTime: Long get() = backupRepository.getLastExportTime()

    /** Path of the most recent safety backup, or null. */
    val lastSafetyBackupPath: String? get() = backupRepository.getLastSafetyBackupPath()

    // ── Export ────────────────────────────────────────────────────────────────

    /**
     * Called by the screen when the SAF [ACTION_CREATE_DOCUMENT] launcher
     * returns a writable [uri].  Runs export on a background coroutine.
     */
    fun exportToUri(uri: Uri) {
        if (_uiState.value is SettingsUiState.Exporting) return   // guard double-tap
        _uiState.value = SettingsUiState.Exporting
        viewModelScope.launch {
            when (val result = backupRepository.exportBackup(uri)) {
                is BackupResult.Success ->
                    _uiState.value = SettingsUiState.Done("Backup exported successfully")
                is BackupResult.Error ->
                    _uiState.value = SettingsUiState.Error(result.message)
            }
        }
    }

    // ── Import (read + validate) ──────────────────────────────────────────────

    /**
     * Called by the screen when the SAF [ACTION_OPEN_DOCUMENT] launcher returns a [uri].
     * Reads, parses, and validates the file, then transitions to [SettingsUiState.ImportReady]
     * which triggers the confirmation dialog.
     */
    fun readImportFile(uri: Uri) {
        _uiState.value = SettingsUiState.ImportParsing
        viewModelScope.launch {
            when (val result = backupRepository.readBackupFromUri(uri)) {
                is BackupReadResult.Ready ->
                    _uiState.value = SettingsUiState.ImportReady(result.preview)
                is BackupReadResult.Error ->
                    _uiState.value = SettingsUiState.Error(result.message)
            }
        }
    }

    // ── Confirm restore ───────────────────────────────────────────────────────

    /**
     * Called when the user taps "Restore" in the confirmation dialog.
     * Reads the backup that was already validated in [readImportFile] and runs the
     * full-replace transaction.
     */
    fun confirmRestore() {
        val preview = (_uiState.value as? SettingsUiState.ImportReady)?.preview ?: return
        _uiState.value = SettingsUiState.Restoring
        viewModelScope.launch {
            when (val result = backupRepository.restore(preview.backup)) {
                is BackupResult.Success ->
                    _uiState.value = SettingsUiState.Done("Restore completed successfully")
                is BackupResult.Error ->
                    _uiState.value = SettingsUiState.Error(result.message)
            }
        }
    }

    // ── Dialog dismissal / state resets ──────────────────────────────────────

    /** Dismiss the confirmation dialog — return to Idle without restoring. */
    fun cancelRestore() { _uiState.value = SettingsUiState.Idle }

    /** Acknowledge a [SettingsUiState.Done] or [SettingsUiState.Error] message. */
    fun clearMessage() { _uiState.value = SettingsUiState.Idle }
}
