package com.kulhad.manager.ui.screens.workers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kulhad.manager.data.util.DateUtils
import com.kulhad.manager.domain.model.AuditDisplay
import com.kulhad.manager.domain.model.Worker
import com.kulhad.manager.ui.components.AuditInfoCard
import com.kulhad.manager.ui.components.BadgeType
import com.kulhad.manager.ui.components.KulhadTopBar
import com.kulhad.manager.ui.components.StatusBadge
import com.kulhad.manager.ui.components.WorkingDateChip
import com.kulhad.manager.ui.theme.BgDeep
import com.kulhad.manager.ui.theme.ErrorRed
import com.kulhad.manager.ui.theme.OverlayWhite07
import com.kulhad.manager.ui.theme.PrimaryBlue
import com.kulhad.manager.ui.theme.Success
import com.kulhad.manager.ui.theme.SurfaceCard
import com.kulhad.manager.ui.theme.TextPrimary
import com.kulhad.manager.ui.theme.TextSecondary

@Composable
fun AttendanceHistoryScreen(
    onBack: () -> Unit,
    viewModel: WorkerViewModel = hiltViewModel()
) {
    val uiState    by viewModel.attendanceHistory.collectAsStateWithLifecycle()
    val allWorkers by viewModel.activeWorkers.collectAsStateWithLifecycle()
    val workingDate by viewModel.workingDate.collectAsStateWithLifecycle()

    // ── Dialog state ──────────────────────────────────────────────────────────
    // editTarget: the row currently being edited; null = dialog closed
    var editTarget    by remember { mutableStateOf<AttendanceUi?>(null) }
    var editIsPresent by remember { mutableStateOf(true) }

    // ── Screen ────────────────────────────────────────────────────────────────
    Column(modifier = Modifier.fillMaxSize().background(BgDeep)) {
        KulhadTopBar(title = "Attendance History", onBack = onBack)

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Working date chip — shared singleton, stays in sync with AttendanceScreen
            item {
                WorkingDateChip(
                    selectedDate = workingDate,
                    onDateSelected = { viewModel.setWorkingDate(it) }
                )
            }

            // Worker filter dropdown
            item {
                WorkerFilterDropdown(
                    workers = allWorkers,
                    selectedWorkerId = uiState.selectedWorkerId,
                    onSelect = { viewModel.setWorkerFilter(it) }
                )
            }

            // Attendance list
            if (uiState.attendance.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No attendance records for this date",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(SurfaceCard)
                            .padding(horizontal = 12.dp)
                    ) {
                        uiState.attendance.forEachIndexed { idx, rec ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        // Open edit dialog and seed it with current status
                                        editTarget    = rec
                                        editIsPresent = rec.isPresent
                                    }
                                    .padding(vertical = 13.dp),
                                verticalAlignment   = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text     = rec.workerName,
                                    color    = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.W500,
                                    modifier = Modifier.weight(1f)
                                )
                                StatusBadge(
                                    text = if (rec.isPresent) "Present" else "Absent",
                                    type = if (rec.isPresent) BadgeType.SUCCESS else BadgeType.ERROR
                                )
                            }
                            if (idx < uiState.attendance.lastIndex) {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(0.5.dp)
                                        .background(OverlayWhite07)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Edit attendance dialog ────────────────────────────────────────────────
    editTarget?.let { target ->
        AttendanceEditDialog(
            workerName   = target.workerName,
            date         = target.date,
            isPresent    = editIsPresent,
            audit        = target.audit,
            onToggle     = { editIsPresent = it },
            onDismiss    = { editTarget = null },
            onSave       = {
                viewModel.updateAttendance(target.workerId, target.date, editIsPresent)
                editTarget = null
            }
        )
    }
}

// ── Worker filter dropdown ────────────────────────────────────────────────────

/**
 * A tappable card row that opens a [DropdownMenu] with "All Workers" + one item per
 * active worker. Dismisses itself on any selection.
 */
@Composable
private fun WorkerFilterDropdown(
    workers: List<Worker>,
    selectedWorkerId: Long?,
    onSelect: (Long?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val selectedName: String = selectedWorkerId
        ?.let { id -> workers.firstOrNull { it.id == id }?.name }
        ?: "All Workers"

    Box {
        // Trigger row — styled as a SurfaceCard chip consistent with WorkingDateChip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceCard)
                .clickable { expanded = true }
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector  = Icons.Outlined.Person,
                contentDescription = "Filter by worker",
                tint   = TextSecondary,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text     = selectedName,
                color    = TextPrimary,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector  = Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint   = TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }

        // Dropdown — styled with SurfaceCard to match dark theme
        DropdownMenu(
            expanded          = expanded,
            onDismissRequest  = { expanded = false },
            modifier          = Modifier.background(SurfaceCard)
        ) {
            DropdownMenuItem(
                text    = { Text("All Workers", color = TextPrimary, fontSize = 14.sp) },
                onClick = { onSelect(null); expanded = false }
            )
            workers.forEach { w ->
                DropdownMenuItem(
                    text    = { Text(w.name, color = TextPrimary, fontSize = 14.sp) },
                    onClick = { onSelect(w.id); expanded = false }
                )
            }
        }
    }
}

// ── Edit attendance dialog ────────────────────────────────────────────────────

/**
 * Modal dialog for changing a worker's attendance status on a specific date.
 *
 * Design constraints:
 *  - No delete option — only Present / Absent toggle
 *  - Save updates the existing row via [WorkerViewModel.updateAttendance]
 *  - Dialog state ([isPresent] + toggle) is owned by the parent composable so it
 *    survives recomposition without being reset
 */
@Composable
private fun AttendanceEditDialog(
    workerName: String,
    date: Long,
    isPresent: Boolean,
    audit: AuditDisplay,
    onToggle: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = SurfaceCard,
        title = {
            Text(
                text       = "Edit Attendance",
                color      = TextPrimary,
                fontSize   = 17.sp,
                fontWeight = FontWeight.W600
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Worker + date info
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Worker:", color = TextSecondary, fontSize = 13.sp)
                        Text(workerName, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.W500)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Date:", color = TextSecondary, fontSize = 13.sp)
                        Text(DateUtils.formatDay(date), color = TextPrimary, fontSize = 13.sp)
                    }
                }

                // Radio buttons — Present / Absent
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggle(true) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RadioButton(
                            selected = isPresent,
                            onClick  = { onToggle(true) },
                            colors   = RadioButtonDefaults.colors(
                                selectedColor   = Success,
                                unselectedColor = TextSecondary
                            )
                        )
                        Text("Present", color = if (isPresent) Success else TextPrimary, fontSize = 15.sp)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggle(false) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RadioButton(
                            selected = !isPresent,
                            onClick  = { onToggle(false) },
                            colors   = RadioButtonDefaults.colors(
                                selectedColor   = ErrorRed,
                                unselectedColor = TextSecondary
                            )
                        )
                        Text("Absent", color = if (!isPresent) ErrorRed else TextPrimary, fontSize = 15.sp)
                    }
                }

                // Audit info — read-only, always shown
                AuditInfoCard(audit = audit)
            }
        },
        confirmButton = {
            TextButton(onClick = onSave) {
                Text("Save", color = PrimaryBlue, fontSize = 15.sp, fontWeight = FontWeight.W500)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary, fontSize = 15.sp)
            }
        }
    )
}
