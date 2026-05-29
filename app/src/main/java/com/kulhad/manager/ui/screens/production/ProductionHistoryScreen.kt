package com.kulhad.manager.ui.screens.production

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
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kulhad.manager.data.util.DateUtils
import com.kulhad.manager.data.util.Money
import com.kulhad.manager.data.util.toDisplay
import com.kulhad.manager.domain.model.ProductionEntry
import com.kulhad.manager.ui.components.AuditInfoCard
import com.kulhad.manager.ui.components.KpiStrip
import com.kulhad.manager.ui.components.KulhadTopBar
import com.kulhad.manager.ui.components.SectionHeader
import com.kulhad.manager.ui.components.WorkerAvatar
import com.kulhad.manager.ui.components.WorkingDateChip
import com.kulhad.manager.ui.theme.BgDeep
import com.kulhad.manager.ui.theme.OverlayWhite07
import com.kulhad.manager.ui.theme.PrimaryBlue
import com.kulhad.manager.ui.theme.PurpleAccent
import com.kulhad.manager.ui.theme.Success
import com.kulhad.manager.ui.theme.SurfaceCard
import com.kulhad.manager.ui.theme.TextPrimary
import com.kulhad.manager.ui.theme.TextSecondary
import com.kulhad.manager.ui.theme.WarningAmber

// ── Production History Screen ─────────────────────────────────────────────────

/**
 * Displays all production entries for the globally selected working date.
 *
 * Driven by [ProductionViewModel.historyDayEntries] which reacts to
 * [com.kulhad.manager.di.WorkingDateManager] via [flatMapLatest] — stale data from a
 * previously viewed date cannot bleed through on date changes.
 *
 * Tapping a row opens [ProductionDetailDialog] (view-only, no edit/delete).
 */
@Composable
fun ProductionHistoryScreen(
    onBack: () -> Unit,
    viewModel: ProductionViewModel = hiltViewModel()
) {
    val entries     by viewModel.historyDayEntries.collectAsStateWithLifecycle()
    val workingDate by viewModel.workingDate.collectAsStateWithLifecycle()

    var selectedEntry by remember { mutableStateOf<ProductionEntry?>(null) }

    // ── Detail dialog ─────────────────────────────────────────────────────────
    selectedEntry?.let { e ->
        ProductionDetailDialog(
            entry     = e,
            onDismiss = { selectedEntry = null }
        )
    }

    // ── Derived day totals ────────────────────────────────────────────────────
    val totalNet      = entries.sumOf { it.netQty }
    val totalDefective = entries.sumOf { it.defectiveQuantity }

    Column(modifier = Modifier.fillMaxSize().background(BgDeep)) {
        KulhadTopBar(title = "Production History", onBack = onBack)

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Working date chip — shared singleton stays in sync across all screens
            item {
                WorkingDateChip(
                    selectedDate  = workingDate,
                    onDateSelected = { viewModel.setWorkingDate(it) }
                )
            }

            // KPI strip for selected date
            item {
                KpiStrip(
                    items = listOf(
                        Triple(totalNet.toString(),        "Net pieces",  PurpleAccent),
                        Triple(totalDefective.toString(),  "Defective",   WarningAmber),
                        Triple(entries.size.toString(),    "Entries",     TextPrimary)
                    )
                )
            }

            // ── Entry list ────────────────────────────────────────────────────
            if (entries.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text  = "No production entries for this date",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                item { SectionHeader(text = "Entries — ${entries.size}") }
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(SurfaceCard)
                            .padding(horizontal = 12.dp)
                    ) {
                        entries.forEachIndexed { idx, e ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedEntry = e }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                WorkerAvatar(name = e.workerName, size = 36.dp, fontSize = 11)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text       = e.workerName,
                                        color      = TextPrimary,
                                        fontSize   = 14.sp,
                                        fontWeight = FontWeight.W500
                                    )
                                    Text(
                                        text  = "${e.productSize}ml",
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text  = "Net ${e.netQty} • def ${e.defectiveQuantity}",
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                                Text(
                                    text       = Money.formatRupeesDouble(e.earnings),
                                    color      = Success,
                                    fontSize   = 13.sp,
                                    fontWeight = FontWeight.W600
                                )
                            }
                            if (idx < entries.lastIndex) {
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
}

// ── Production detail dialog ──────────────────────────────────────────────────

/**
 * View-only dialog showing the full detail of a single [entry].
 *
 * Design constraints:
 *  - No edit or delete actions.
 *  - "Close" is the only button. Tap-outside and hardware back also dismiss.
 *  - [AuditInfoCard] renders createdAt as "—" for migrated rows (createdAt == 0L).
 *  - Production entries have no remark column in the schema; none is shown.
 */
@Composable
private fun ProductionDetailDialog(
    entry: ProductionEntry,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = SurfaceCard,
        title = {
            Text(
                text       = "Production Details",
                color      = TextPrimary,
                fontSize   = 17.sp,
                fontWeight = FontWeight.W600
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

                // ── Product size (hero value) ──────────────────────────────
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text          = "PRODUCT",
                        color         = TextSecondary,
                        fontSize      = 10.sp,
                        fontWeight    = FontWeight.W600,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text       = "${entry.productSize}ml",
                        color      = PurpleAccent,
                        fontSize   = 22.sp,
                        fontWeight = FontWeight.W600
                    )
                }

                // ── Worker ────────────────────────────────────────────────
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Worker:", color = TextSecondary, fontSize = 13.sp)
                    Text(
                        text       = entry.workerName,
                        color      = TextPrimary,
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.W500
                    )
                }

                // ── Quantity breakdown ─────────────────────────────────────
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text          = "PRODUCED",
                            color         = TextSecondary,
                            fontSize      = 10.sp,
                            letterSpacing = 0.6.sp
                        )
                        Text(
                            text       = entry.quantityProduced.toString(),
                            color      = TextPrimary,
                            fontSize   = 15.sp,
                            fontWeight = FontWeight.W500
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text          = "DEFECTIVE",
                            color         = TextSecondary,
                            fontSize      = 10.sp,
                            letterSpacing = 0.6.sp
                        )
                        Text(
                            text       = entry.defectiveQuantity.toString(),
                            color      = WarningAmber,
                            fontSize   = 15.sp,
                            fontWeight = FontWeight.W500
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text          = "NET",
                            color         = TextSecondary,
                            fontSize      = 10.sp,
                            letterSpacing = 0.6.sp
                        )
                        Text(
                            text       = entry.netQty.toString(),
                            color      = PurpleAccent,
                            fontSize   = 15.sp,
                            fontWeight = FontWeight.W500
                        )
                    }
                }

                // ── Earnings ──────────────────────────────────────────────
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Earnings:", color = TextSecondary, fontSize = 13.sp)
                    Text(
                        text       = Money.formatRupeesDouble(entry.earnings),
                        color      = Success,
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.W500
                    )
                }

                // ── Date ──────────────────────────────────────────────────
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Date:", color = TextSecondary, fontSize = 13.sp)
                    Text(DateUtils.formatDay(entry.date), color = TextPrimary, fontSize = 13.sp)
                }

                // ── Audit trail ───────────────────────────────────────────
                AuditInfoCard(audit = entry.audit.toDisplay())
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = PrimaryBlue, fontSize = 15.sp, fontWeight = FontWeight.W500)
            }
        }
    )
}
