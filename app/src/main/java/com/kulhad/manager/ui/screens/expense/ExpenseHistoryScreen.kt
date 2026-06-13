package com.kulhad.manager.ui.screens.expense

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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
import com.kulhad.manager.ui.components.KulhadButton
import com.kulhad.manager.ui.components.KulhadTextField
import com.kulhad.manager.ui.components.KulhadTopBar
import com.kulhad.manager.ui.components.SectionHeader
import com.kulhad.manager.ui.components.StatusBadge
import com.kulhad.manager.ui.theme.BgDeep
import com.kulhad.manager.ui.theme.OverlayWhite07
import com.kulhad.manager.ui.theme.PrimaryBlue
import com.kulhad.manager.ui.theme.PrimaryBlueDark
import com.kulhad.manager.ui.theme.PrimaryBlueLight
import com.kulhad.manager.ui.theme.Success
import com.kulhad.manager.ui.theme.SurfaceCard
import com.kulhad.manager.ui.theme.TextPrimary
import com.kulhad.manager.ui.theme.TextSecondary
import com.kulhad.manager.ui.theme.TextTertiary
import com.kulhad.manager.ui.theme.WarningAmber

// ── Expense History Screen ────────────────────────────────────────────────────

/**
 * Month-wise expense summary screen.
 *
 * Shows the selected calendar month with:
 *  - Month navigation arrows (future months blocked)
 *  - Total expenses summary card (total, category count, entry count)
 *  - Category breakdown sorted by amount descending
 *  - Expandable detail list (all entries for the month, date descending)
 *  - Tap-to-edit via [ExpenseEditDialog] (preserved from previous implementation)
 *
 * Driven by [ExpenseViewModel.historyMonthData] which uses [flatMapLatest] over
 * [ExpenseViewModel.historyMonthAnchor] — switching months cancels the previous
 * DB subscription immediately.
 */
@Composable
fun ExpenseHistoryScreen(
    onBack: () -> Unit,
    viewModel: ExpenseViewModel = hiltViewModel()
) {
    val monthData      by viewModel.historyMonthData.collectAsStateWithLifecycle()
    val atCurrentMonth by viewModel.historyAtCurrentMonth.collectAsStateWithLifecycle()
    val allTypes       by viewModel.expenseTypes.collectAsStateWithLifecycle()

    var selectedExpense  by remember { mutableStateOf<Expense?>(null) }
    var detailsExpanded  by remember { mutableStateOf(false) }

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

    Column(modifier = Modifier.fillMaxSize().background(BgDeep)) {
        KulhadTopBar(
            title   = "Expense History",
            onBack  = onBack,
            actions = {
                // ← Previous month
                Icon(
                    imageVector        = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Previous month",
                    tint               = TextPrimary,
                    modifier           = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { viewModel.historyPrevMonth() }
                )
                // Month label
                Text(
                    text       = DateUtils.formatMonthFull(monthData.monthAnchor),
                    color      = TextPrimary,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.W600,
                    textAlign  = TextAlign.Center,
                    modifier   = Modifier.padding(horizontal = 6.dp)
                )
                // → Next month (disabled when at current month)
                Icon(
                    imageVector        = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next month",
                    tint               = if (atCurrentMonth) TextTertiary else TextPrimary,
                    modifier           = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(enabled = !atCurrentMonth) { viewModel.historyNextMonth() }
                )
            }
        )

        LazyColumn(
            contentPadding      = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            // ── Summary card ──────────────────────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceCard)
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text      = "Total Expenses",
                        color     = TextSecondary,
                        fontSize  = 13.sp,
                        letterSpacing = 0.4.sp
                    )
                    Text(
                        text       = Money.formatRupees(monthData.total.toLong()),
                        color      = WarningAmber,
                        fontSize   = 28.sp,
                        fontWeight = FontWeight.W700
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        SummaryPill(label = "Categories", value = monthData.categoryCount.toString())
                        SummaryPill(label = "Entries",    value = monthData.entryCount.toString())
                    }
                }
            }

            // ── Empty state ───────────────────────────────────────────────
            if (monthData.entries.isEmpty()) {
                item {
                    Box(
                        modifier            = Modifier.fillMaxWidth().padding(top = 40.dp),
                        contentAlignment    = Alignment.Center
                    ) {
                        Text(
                            text     = "No expenses for ${DateUtils.formatMonthFull(monthData.monthAnchor)}",
                            color    = TextSecondary,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {

                // ── Category breakdown ────────────────────────────────────
                item { SectionHeader(text = "By Category") }
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(SurfaceCard)
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        monthData.breakdown.forEachIndexed { idx, (category, amount) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 13.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment     = Alignment.CenterVertically
                            ) {
                                Text(
                                    text       = category,
                                    color      = TextPrimary,
                                    fontSize   = 15.sp,
                                    fontWeight = FontWeight.W500,
                                    modifier   = Modifier.weight(1f)
                                )
                                Text(
                                    text       = Money.formatRupees(amount.toLong()),
                                    color      = WarningAmber,
                                    fontSize   = 15.sp,
                                    fontWeight = FontWeight.W600
                                )
                            }
                            if (idx < monthData.breakdown.lastIndex) {
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

                // ── Expandable detail entries ─────────────────────────────
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceCard)
                            .clickable { detailsExpanded = !detailsExpanded }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text(
                            text       = if (detailsExpanded) "Hide Details" else "View Details",
                            color      = PrimaryBlue,
                            fontSize   = 14.sp,
                            fontWeight = FontWeight.W600
                        )
                        Icon(
                            imageVector        = if (detailsExpanded)
                                Icons.Default.KeyboardArrowUp
                            else
                                Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint               = PrimaryBlue,
                            modifier           = Modifier.size(20.dp)
                        )
                    }
                }

                item {
                    AnimatedVisibility(
                        visible = detailsExpanded,
                        enter   = expandVertically(),
                        exit    = shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(SurfaceCard)
                                .padding(horizontal = 12.dp)
                        ) {
                            monthData.entries.forEachIndexed { idx, expense ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedExpense = expense }
                                        .padding(vertical = 11.dp),
                                    verticalAlignment     = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Date column
                                    Text(
                                        text     = DateUtils.formatDayShort(expense.date),
                                        color    = TextSecondary,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(end = 2.dp)
                                    )
                                    // Category badge + remark
                                    Column(modifier = Modifier.weight(1f)) {
                                        StatusBadge(expense.typeName, BadgeType.PURPLE)
                                        if (expense.remark.isNotBlank()) {
                                            Text(
                                                text     = expense.remark,
                                                color    = TextSecondary,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                    // Amount
                                    Text(
                                        text       = Money.formatRupees(expense.amount.toLong()),
                                        color      = TextPrimary,
                                        fontSize   = 14.sp,
                                        fontWeight = FontWeight.W600
                                    )
                                }
                                if (idx < monthData.entries.lastIndex) {
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
}

// ── Private helpers ───────────────────────────────────────────────────────────

@Composable
private fun SummaryPill(label: String, value: String) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text       = value,
            color      = TextPrimary,
            fontSize   = 14.sp,
            fontWeight = FontWeight.W600
        )
        Text(
            text     = label,
            color    = TextSecondary,
            fontSize = 13.sp
        )
    }
}

// ── Expense edit dialog ───────────────────────────────────────────────────────

/**
 * Edit dialog for a single [expense].
 *
 * Non-editable context (date) is shown as static text.
 * Editable fields: amount, remark, and expense type (category chips).
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
                                repeat(3 - rowTypes.size) { Box(modifier = Modifier.weight(1f)) }
                            }
                        }
                    }
                }

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
