package com.kulhad.manager.ui.screens.expense

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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kulhad.manager.data.util.DateUtils
import com.kulhad.manager.data.util.Money
import com.kulhad.manager.data.util.toDisplay
import com.kulhad.manager.domain.model.Expense
import com.kulhad.manager.domain.model.ExpenseType
import com.kulhad.manager.ui.components.AuditInfoCard
import com.kulhad.manager.ui.components.BadgeType
import com.kulhad.manager.ui.components.KpiStrip
import com.kulhad.manager.ui.components.KulhadButton
import com.kulhad.manager.ui.components.KulhadTextField
import com.kulhad.manager.ui.components.KulhadTopBar
import com.kulhad.manager.ui.components.SectionHeader
import com.kulhad.manager.ui.components.StatusBadge
import com.kulhad.manager.ui.components.WorkingDateChip
import com.kulhad.manager.ui.theme.BgDeep
import com.kulhad.manager.ui.theme.OverlayWhite07
import com.kulhad.manager.ui.theme.OverlayWhite15
import com.kulhad.manager.ui.theme.PrimaryBlue
import com.kulhad.manager.ui.theme.PrimaryBlueDark
import com.kulhad.manager.ui.theme.PrimaryBlueLight
import com.kulhad.manager.ui.theme.SurfaceCard
import com.kulhad.manager.ui.theme.TextPrimary
import com.kulhad.manager.ui.theme.TextSecondary
import com.kulhad.manager.ui.theme.WarningAmber

// ── Expense History Screen ────────────────────────────────────────────────────

/**
 * Date-based view of all expense rows for the globally selected working date.
 * Reacts automatically when the working date changes — no manual refresh.
 *
 * Driven by [ExpenseViewModel.historyDayExpenses] which uses [flatMapLatest] over
 * [WorkingDateManager.currentWorkingDate] — stale rows from a previously viewed date
 * cannot bleed through on date changes.
 *
 * Tapping a row opens [ExpenseEditDialog] which allows editing amount, remark, and
 * expense type. Date is shown as non-editable context.
 */
@Composable
fun ExpenseHistoryScreen(
    onBack: () -> Unit,
    viewModel: ExpenseViewModel = hiltViewModel()
) {
    val entries     by viewModel.historyDayExpenses.collectAsStateWithLifecycle()
    val workingDate by viewModel.workingDate.collectAsStateWithLifecycle()
    val allTypes    by viewModel.expenseTypes.collectAsStateWithLifecycle()

    var selectedExpense by remember { mutableStateOf<Expense?>(null) }

    // ── Edit dialog ────────────────────────────────────────────────────────
    selectedExpense?.let { expense ->
        ExpenseEditDialog(
            expense   = expense,
            allTypes  = allTypes,
            onDismiss = { selectedExpense = null },
            onSave    = { typeId, amount, remark ->
                viewModel.updateExpense(
                    id     = expense.id,
                    typeId = typeId,
                    amount = amount,
                    remark = remark,
                    onDone = { selectedExpense = null }
                )
            }
        )
    }

    // ── Derived KPIs ──────────────────────────────────────────────────────
    val total = entries.sumOf { it.amount }

    Column(modifier = Modifier.fillMaxSize().background(BgDeep)) {
        KulhadTopBar(title = "Expense History", onBack = onBack)

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Working date chip — shared singleton, synced across all screens
            item {
                WorkingDateChip(
                    selectedDate   = workingDate,
                    onDateSelected = { viewModel.setWorkingDate(it) }
                )
            }

            // KPI strip
            item {
                KpiStrip(
                    items = listOf(
                        Triple(entries.size.toString(),        "Entries",    TextPrimary),
                        Triple(Money.formatRupees(total.toLong()), "Total",  WarningAmber)
                    )
                )
            }

            // ── Entry list ────────────────────────────────────────────────
            if (entries.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text     = "No expenses for this date",
                            color    = TextSecondary,
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
                        entries.forEachIndexed { idx, expense ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedExpense = expense }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text       = Money.formatRupees(expense.amount.toLong()),
                                        color      = TextPrimary,
                                        fontSize   = 15.sp,
                                        fontWeight = FontWeight.W600
                                    )
                                    if (expense.remark.isNotBlank()) {
                                        Text(
                                            text     = expense.remark,
                                            color    = TextSecondary,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Text(
                                        text     = DateUtils.formatTime(expense.date),
                                        color    = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                                StatusBadge(expense.typeName, BadgeType.PURPLE)
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

// ── Expense edit dialog ───────────────────────────────────────────────────────

/**
 * Edit dialog for a single [expense].
 *
 * Non-editable context (date) is shown as static text.
 * Editable fields: amount, remark, and expense type (category chips).
 *
 * [AuditInfoCard] shows CREATED and LAST UPDATED sections.
 * Since expenses are editable, LAST UPDATED will show after the first save.
 *
 * Save is disabled until amount is a positive integer and a type is selected.
 * Dismiss: tap-outside, hardware back, or the "Close" button.
 */
@Composable
private fun ExpenseEditDialog(
    expense: Expense,
    allTypes: List<ExpenseType>,
    onDismiss: () -> Unit,
    onSave: (typeId: Long, amount: Int, remark: String) -> Unit
) {
    var selectedTypeId by remember(expense.id) { mutableStateOf(expense.typeId) }
    var amount         by remember(expense.id) { mutableStateOf(expense.amount.toString()) }
    var remark         by remember(expense.id) { mutableStateOf(expense.remark) }

    val amtInt  = amount.toIntOrNull() ?: 0
    val canSave = amtInt > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = SurfaceCard,
        title = {
            Text(
                text       = "Edit Expense",
                color      = TextPrimary,
                fontSize   = 17.sp,
                fontWeight = FontWeight.W600
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                // ── Date (non-editable) ───────────────────────────────────
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Date:", color = TextSecondary, fontSize = 13.sp)
                    Text(
                        DateUtils.formatDay(expense.date),
                        color = TextPrimary, fontSize = 13.sp
                    )
                }

                // ── Type / category picker ────────────────────────────────
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text          = "EXPENSE TYPE",
                        color         = TextSecondary,
                        fontSize      = 10.sp,
                        fontWeight    = FontWeight.W600,
                        letterSpacing = 0.6.sp
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        allTypes.chunked(3).forEach { rowTypes ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                rowTypes.forEach { t ->
                                    val sel = selectedTypeId == t.id
                                    Text(
                                        text       = t.name,
                                        color      = if (sel) PrimaryBlueLight else TextPrimary,
                                        fontSize   = 12.sp,
                                        fontWeight = if (sel) FontWeight.W600 else FontWeight.W500,
                                        textAlign  = TextAlign.Center,
                                        modifier   = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (sel) PrimaryBlueDark else BgDeep)
                                            .clickable { selectedTypeId = t.id }
                                            .padding(vertical = 8.dp, horizontal = 6.dp)
                                    )
                                }
                                // Pad remaining slots
                                repeat(3 - rowTypes.size) {
                                    Box(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                // ── Editable fields ───────────────────────────────────────
                KulhadTextField(
                    label         = "Amount (₹)",
                    value         = amount,
                    onValueChange = { amount = it.filter { ch -> ch.isDigit() } },
                    keyboardType  = KeyboardType.Number
                )

                KulhadTextField(
                    label         = "Remark",
                    value         = remark,
                    onValueChange = { remark = it }
                )

                // ── Audit trail ───────────────────────────────────────────
                AuditInfoCard(audit = expense.audit.toDisplay())
            }
        },
        confirmButton = {
            KulhadButton(
                text    = "Save",
                enabled = canSave,
                onClick = { onSave(selectedTypeId, amtInt, remark) }
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = PrimaryBlue, fontSize = 15.sp, fontWeight = FontWeight.W500)
            }
        }
    )
}
