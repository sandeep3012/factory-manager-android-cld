package com.kulhad.manager.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kulhad.manager.data.util.DateUtils
import com.kulhad.manager.ui.components.KulhadButton
import com.kulhad.manager.ui.components.KulhadTopBar
import com.kulhad.manager.ui.components.SectionHeader
import com.kulhad.manager.ui.theme.BgDeep
import com.kulhad.manager.ui.theme.ErrorRed
import com.kulhad.manager.ui.theme.InfoBlue
import com.kulhad.manager.ui.theme.OverlayWhite07
import com.kulhad.manager.ui.theme.PrimaryBlue
import com.kulhad.manager.ui.theme.Success
import com.kulhad.manager.ui.theme.SurfaceCard
import com.kulhad.manager.ui.theme.TextPrimary
import com.kulhad.manager.ui.theme.TextSecondary
import com.kulhad.manager.ui.theme.TextTertiary
import com.kulhad.manager.ui.theme.WarningAmber

// ── Settings Screen ───────────────────────────────────────────────────────────

/**
 * Settings hub.  Phase 1 contains a single Data Management section with:
 *  - Export Backup
 *  - Restore Backup
 *  - Last Backup Time (if a prior export exists)
 *
 * Navigation from other sections can be added here in future phases (e.g. appearance,
 * notifications, account).
 *
 * SAF launchers live here (they must be created inside a Composable) — the ViewModel
 * receives the URI after the picker returns and handles all async work.
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state       by viewModel.uiState.collectAsStateWithLifecycle()
    val lastExport  = viewModel.lastExportTime

    // ── SAF: export — suggests the default filename; user can rename it ───────
    val exportLauncher = rememberLauncherForActivityResult(
        contract = CreateDocument("application/octet-stream")
    ) { uri -> uri?.let { viewModel.exportToUri(it) } }

    // ── SAF: import ───────────────────────────────────────────────────────────
    val importLauncher = rememberLauncherForActivityResult(
        contract = OpenDocument()
    ) { uri -> uri?.let { viewModel.readImportFile(it) } }

    // ── Restore confirmation dialog ───────────────────────────────────────────
    if (state is SettingsUiState.ImportReady) {
        val preview = (state as SettingsUiState.ImportReady).preview
        RestoreConfirmDialog(
            workerCount     = preview.workerCount,
            productionCount = preview.productionCount,
            salesCount      = preview.salesCount,
            expenseCount    = preview.expenseCount,
            createdAt       = preview.createdAt,
            onCancel        = { viewModel.cancelRestore() },
            onConfirm       = { viewModel.confirmRestore() }
        )
    }

    // ── Error dialog ──────────────────────────────────────────────────────────
    if (state is SettingsUiState.Error) {
        val message = (state as SettingsUiState.Error).message
        AlertDialog(
            onDismissRequest = { viewModel.clearMessage() },
            containerColor   = SurfaceCard,
            title = { Text("Error", color = ErrorRed, fontSize = 17.sp, fontWeight = FontWeight.W600) },
            text  = { Text(message, color = TextSecondary, fontSize = 14.sp, lineHeight = 20.sp) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearMessage() }) {
                    Text("OK", color = PrimaryBlue, fontSize = 15.sp)
                }
            }
        )
    }

    // ── Done snackbar (simple modal banner) ───────────────────────────────────
    if (state is SettingsUiState.Done) {
        val message = (state as SettingsUiState.Done).message
        AlertDialog(
            onDismissRequest = { viewModel.clearMessage() },
            containerColor   = SurfaceCard,
            title = { Text("Done", color = Success, fontSize = 17.sp, fontWeight = FontWeight.W600) },
            text  = { Text(message, color = TextSecondary, fontSize = 14.sp) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearMessage() }) {
                    Text("OK", color = PrimaryBlue, fontSize = 15.sp)
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(BgDeep)) {
        KulhadTopBar(title = "Settings", onBack = onBack)

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ── Data Management section ───────────────────────────────────────
            item { SectionHeader(text = "Data Management") }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceCard)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Last backup info
                    if (lastExport > 0L) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Last Backup", color = TextSecondary, fontSize = 13.sp)
                            Text(
                                DateUtils.formatAuditTimestamp(lastExport),
                                color = TextTertiary, fontSize = 12.sp
                            )
                        }
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(0.5.dp)
                                .background(OverlayWhite07)
                        )
                    }

                    // Export
                    DataActionRow(
                        icon     = Icons.Outlined.CloudUpload,
                        iconTint = InfoBlue,
                        title    = "Export Backup",
                        subtitle = "Save all data to a .kulhad file",
                        loading  = state is SettingsUiState.Exporting,
                        onClick  = {
                            val name = "kulhad_backup_${DateUtils.formatForFilename()}.kulhad"
                            exportLauncher.launch(name)
                        }
                    )

                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .background(OverlayWhite07)
                    )

                    // Restore
                    DataActionRow(
                        icon     = Icons.Outlined.CloudDownload,
                        iconTint = WarningAmber,
                        title    = "Restore Backup",
                        subtitle = "Replace all data from a .kulhad file",
                        loading  = state is SettingsUiState.ImportParsing ||
                                   state is SettingsUiState.Restoring,
                        onClick  = { importLauncher.launch(arrayOf("*/*")) }
                    )
                }
            }

            // ── Storage info ─────────────────────────────────────────────────
            val safetyPath = viewModel.lastSafetyBackupPath
            if (safetyPath != null) {
                item { SectionHeader(text = "Safety Backup") }
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(SurfaceCard)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment     = Alignment.Top
                        ) {
                            Icon(
                                Icons.Outlined.Storage,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Column {
                                Text(
                                    "Automatic backup before last restore",
                                    color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.W500
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    safetyPath,
                                    color = TextTertiary, fontSize = 11.sp, lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Restore confirmation dialog ───────────────────────────────────────────────

@Composable
private fun RestoreConfirmDialog(
    workerCount:     Int,
    productionCount: Int,
    salesCount:      Int,
    expenseCount:    Int,
    createdAt:       Long,
    onCancel:        () -> Unit,
    onConfirm:       () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        containerColor   = SurfaceCard,
        title = {
            Text(
                "Restore Backup",
                color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.W600
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Backup created: ${DateUtils.formatAuditTimestamp(createdAt)}",
                    color = TextSecondary, fontSize = 13.sp
                )

                Box(
                    Modifier.fillMaxWidth().height(0.5.dp).background(OverlayWhite07)
                )

                // Stats
                listOf(
                    "Workers"            to workerCount,
                    "Production Entries" to productionCount,
                    "Sales"              to salesCount,
                    "Expenses"           to expenseCount
                ).forEach { (label, count) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(label, color = TextSecondary, fontSize = 13.sp)
                        Text("$count", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.W500)
                    }
                }

                Box(
                    Modifier.fillMaxWidth().height(0.5.dp).background(OverlayWhite07)
                )

                Text(
                    "⚠ This will replace ALL current data.\n" +
                    "A safety backup of your current data will be created automatically before restore.",
                    color    = WarningAmber,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        },
        confirmButton = {
            KulhadButton(
                text    = "Restore",
                onClick = onConfirm
            )
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Cancel", color = PrimaryBlue, fontSize = 15.sp, fontWeight = FontWeight.W500)
            }
        }
    )
}

// ── Data action row ───────────────────────────────────────────────────────────

@Composable
private fun DataActionRow(
    icon:     ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    title:    String,
    subtitle: String,
    loading:  Boolean,
    onClick:  () -> Unit
) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier          = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.15f)),
            contentAlignment  = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null,
                tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title,    color = TextPrimary,   fontSize = 14.sp, fontWeight = FontWeight.W500)
            Text(subtitle, color = TextSecondary, fontSize = 12.sp)
        }
        if (loading) {
            CircularProgressIndicator(
                modifier  = Modifier.size(22.dp),
                color     = PrimaryBlue,
                strokeWidth = 2.dp
            )
        } else {
            TextButton(onClick = onClick) {
                Text(
                    text = if (title.startsWith("Export")) "Export" else "Restore",
                    color = PrimaryBlue,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.W500
                )
            }
        }
    }
}
