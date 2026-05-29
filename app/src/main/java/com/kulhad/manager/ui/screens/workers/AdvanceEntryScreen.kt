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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kulhad.manager.data.util.DateUtils
import com.kulhad.manager.data.util.Money
import com.kulhad.manager.data.util.toDisplay
import com.kulhad.manager.domain.model.WorkerAdvanceRecord
import com.kulhad.manager.ui.components.AuditInfoCard
import com.kulhad.manager.ui.components.KpiStrip
import com.kulhad.manager.ui.components.KulhadButton
import com.kulhad.manager.ui.components.KulhadTextField
import com.kulhad.manager.ui.components.KulhadTopBar
import com.kulhad.manager.ui.components.SectionHeader
import com.kulhad.manager.ui.components.WorkingDateChip
import com.kulhad.manager.ui.theme.BgDeep
import com.kulhad.manager.ui.theme.ErrorRed
import com.kulhad.manager.ui.theme.OverlayWhite07
import com.kulhad.manager.ui.theme.PrimaryBlue
import com.kulhad.manager.ui.theme.SurfaceCard
import com.kulhad.manager.ui.theme.TextPrimary
import com.kulhad.manager.ui.theme.TextSecondary
import com.kulhad.manager.ui.theme.WarningAmber

@Composable
fun AdvanceEntryScreen(
    initialWorkerId: Long?,
    onBack: () -> Unit,
    viewModel: WorkerViewModel = hiltViewModel()
) {
    val workers by viewModel.activeWorkers.collectAsStateWithLifecycle()
    val workingDate by viewModel.workingDate.collectAsStateWithLifecycle()
    var selectedId       by remember { mutableStateOf(initialWorkerId) }
    var amount           by remember { mutableStateOf("") }
    var remark           by remember { mutableStateOf("") }
    var selectedAdvance  by remember { mutableStateOf<WorkerAdvanceRecord?>(null) }

    val effectiveId = selectedId ?: workers.firstOrNull()?.id
    val advances by (effectiveId?.let { viewModel.observeAdvances(it) }
        ?: kotlinx.coroutines.flow.flowOf(emptyList())).collectAsStateWithLifecycle(emptyList())
    val monthTotal by (effectiveId?.let { viewModel.observeAdvanceTotalThisMonth(it) }
        ?: kotlinx.coroutines.flow.flowOf(0)).collectAsStateWithLifecycle(0)
    val workerName = workers.firstOrNull { it.id == effectiveId }?.name ?: "Worker"

    // ── Advance detail dialog (view-only, tap outside / back to close) ────────
    selectedAdvance?.let { a ->
        AdvanceDetailDialog(
            advance   = a,
            onDismiss = { selectedAdvance = null }
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(BgDeep)) {
        KulhadTopBar(title = "Advance Entry", onBack = onBack)
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    "SELECT WORKER",
                    color = TextSecondary, fontSize = 14.sp, letterSpacing = 0.6.sp
                )
            }

            // Worker selection — flat rows with dividers
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceCard)
                        .padding(horizontal = 12.dp)
                ) {
                    workers.forEachIndexed { idx, w ->
                        val sel = w.id == effectiveId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedId = w.id }
                                .padding(vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Selection dot
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (sel) WarningAmber else Color.Transparent)
                            )
                            Text(
                                text = w.name,
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = if (sel) FontWeight.W600 else FontWeight.W500,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = if (w.currentType.name == "PIECE") "Piece" else "Salary",
                                color = if (sel) WarningAmber else TextSecondary,
                                fontSize = 14.sp
                            )
                        }
                        if (idx < workers.lastIndex) {
                            Box(Modifier.fillMaxWidth().height(0.5.dp).background(OverlayWhite07))
                        }
                    }
                    if (workers.isEmpty()) {
                        Text(
                            "No workers found",
                            color = TextSecondary, fontSize = 16.sp,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                }
            }

            // Advances this month KPI
            item {
                KpiStrip(
                    items = listOf(
                        Triple(Money.formatRupees(monthTotal.toLong()), "Advances this month", ErrorRed)
                    )
                )
            }

            item {
                KulhadTextField(
                    label = "Amount (₹)",
                    value = amount,
                    onValueChange = { amount = it.filter { ch -> ch.isDigit() } },
                    keyboardType = KeyboardType.Number
                )
            }
            item {
                KulhadTextField(
                    label = "Remark",
                    value = remark,
                    onValueChange = { remark = it }
                )
            }
            // Working date chip — date is read from WorkingDateManager inside saveAdvance()
            item {
                WorkingDateChip(
                    selectedDate = workingDate,
                    onDateSelected = { viewModel.setWorkingDate(it) }
                )
            }
            item {
                KulhadButton(
                    text = "Save Advance",
                    onClick = {
                        val amt = amount.toIntOrNull() ?: return@KulhadButton
                        if (amt <= 0 || effectiveId == null) return@KulhadButton
                        viewModel.saveAdvance(effectiveId, amt, remark) {
                            amount = ""
                            remark = ""
                        }
                    }
                )
            }

            // Advance history — flat rows with dividers
            item {
                SectionHeader(
                    text = "History — $workerName",
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (advances.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No advances yet", color = TextSecondary, fontSize = 14.sp)
                    }
                }
            } else {
                items(advances, key = { it.id }) { a ->
                    val isLast = a == advances.last()
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedAdvance = a }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = Money.formatRupees(a.amount),
                                    color = ErrorRed, fontSize = 16.sp, fontWeight = FontWeight.W500
                                )
                                if (a.remark.isNotBlank()) {
                                    Text(text = a.remark, color = TextSecondary, fontSize = 14.sp)
                                }
                            }
                            Text(
                                text = DateUtils.formatDayShort(a.date),
                                color = TextSecondary, fontSize = 14.sp
                            )
                        }
                        if (!isLast) {
                            Box(Modifier.fillMaxWidth().height(0.5.dp).background(OverlayWhite07))
                        }
                    }
                }
            }
        }
    }
}

// ── Advance detail dialog ─────────────────────────────────────────────────────

/**
 * View-only dialog that surfaces the full details — amount, date, remark, and audit trail —
 * for a single [advance] row.
 *
 * Design constraints:
 *  - No edit or delete actions; the single button is a "Close" dismissal.
 *  - Tap outside ([onDismissRequest]) and hardware back both invoke [onDismiss].
 *  - [AuditInfoCard] renders createdAt as "—" when the row was migrated from v1 (createdAt == 0L).
 *  - Long remarks are handled via [Modifier.weight(1f)] to prevent clipping.
 */
@Composable
private fun AdvanceDetailDialog(
    advance: WorkerAdvanceRecord,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = SurfaceCard,
        title = {
            Text(
                text       = "Advance Details",
                color      = TextPrimary,
                fontSize   = 17.sp,
                fontWeight = FontWeight.W600
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

                // ── Amount ───────────────────────────────────────────────────
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text          = "AMOUNT",
                        color         = TextSecondary,
                        fontSize      = 10.sp,
                        fontWeight    = FontWeight.W600,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text       = Money.formatRupees(advance.amount),
                        color      = ErrorRed,
                        fontSize   = 22.sp,
                        fontWeight = FontWeight.W600
                    )
                }

                // ── Date ─────────────────────────────────────────────────────
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Date:", color = TextSecondary, fontSize = 13.sp)
                    Text(DateUtils.formatDay(advance.date), color = TextPrimary, fontSize = 13.sp)
                }

                // ── Remark (shown only when non-blank; weight handles long text) ──
                if (advance.remark.isNotBlank()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Remark:", color = TextSecondary, fontSize = 13.sp)
                        Text(
                            text     = advance.remark,
                            color    = TextPrimary,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // ── Audit trail ───────────────────────────────────────────────
                AuditInfoCard(audit = advance.audit.toDisplay())
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = PrimaryBlue, fontSize = 15.sp, fontWeight = FontWeight.W500)
            }
        }
    )
}
