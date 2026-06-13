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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.kulhad.manager.domain.model.Worker
import com.kulhad.manager.ui.components.KulhadButton
import com.kulhad.manager.ui.components.KulhadTextField
import com.kulhad.manager.ui.components.KulhadTopBar
import com.kulhad.manager.ui.components.SectionHeader
import com.kulhad.manager.ui.components.SizePillGrid
import com.kulhad.manager.ui.components.WorkingDateChip
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

/**
 * Add / Edit production entry screen.
 *
 * [entryId] == null  →  Add mode (existing behaviour, unchanged).
 * [entryId] != null  →  Edit mode:
 *   - Worker, Product, Date, and Rate are shown as read-only locked fields.
 *   - Only Quantity Produced and Defective Quantity are editable.
 *   - Saving calls [ProductionViewModel.updateEntry] which preserves workerId,
 *     productId, date, and rateSnapshot at the repository layer.
 *   - Attendance is not touched — worker/date are locked, no side-effects needed.
 */
@Composable
fun AddProductionScreen(
    onBack: () -> Unit,
    entryId: Long? = null,
    viewModel: ProductionViewModel = hiltViewModel()
) {
    val isEditMode = entryId != null

    val activeWorkers by viewModel.activeWorkers.collectAsStateWithLifecycle()
    val products      by viewModel.productsWithRates.collectAsStateWithLifecycle()
    val workingDate   by viewModel.workingDate.collectAsStateWithLifecycle()
    val entryForEdit  by viewModel.entryForEdit.collectAsStateWithLifecycle()

    // ── Form state ───────────────────────────────────────────────────────────
    var workerId  by remember { mutableStateOf<Long?>(null) }
    var sizeMl    by remember { mutableStateOf<Int?>(null) }
    var qty       by remember { mutableStateOf("") }
    var defective by remember { mutableStateOf("0") }
    var rate      by remember { mutableStateOf(0.0) }
    var populated by remember { mutableStateOf(false) }

    // Edit mode — load entry once
    LaunchedEffect(entryId) {
        if (entryId != null) viewModel.loadEntryForEdit(entryId)
    }
    // Populate read-only display fields and editable qty fields when entry arrives
    LaunchedEffect(entryForEdit) {
        val e = entryForEdit
        if (isEditMode && e != null && !populated) {
            workerId  = e.workerId
            sizeMl    = e.productSize
            qty       = e.quantityProduced.toString()
            defective = e.defectiveQuantity.toString()
            rate      = e.rateSnapshot
            populated = true
        }
    }
    // Add mode — default-select first worker, fetch rate when size picked
    LaunchedEffect(activeWorkers) {
        if (!isEditMode && workerId == null) workerId = activeWorkers.firstOrNull()?.id
    }
    LaunchedEffect(sizeMl, products) {
        if (!isEditMode) {
            val pid = products.firstOrNull { it.product.sizeMl == sizeMl }?.product?.id
            rate = pid?.let { viewModel.rateFor(it) } ?: 0.0
        }
    }

    // ── Derived values ───────────────────────────────────────────────────────
    val qtyInt   = qty.toIntOrNull() ?: 0
    val defInt   = defective.toIntOrNull() ?: 0
    val net      = (qtyInt - defInt).coerceAtLeast(0)
    val earnings = net * rate

    // Resolved display strings for edit mode locked fields
    val lockedWorkerName   = entryForEdit?.workerName ?: ""
    val lockedProductLabel = entryForEdit?.productLabel ?: ""
    val lockedDate         = entryForEdit?.let { DateUtils.formatDay(it.date) } ?: ""
    val lockedRate         = "${Money.formatRupeesDouble(rate)} / piece"

    Column(modifier = Modifier.fillMaxSize().background(BgDeep)) {
        KulhadTopBar(
            title  = if (isEditMode) "Edit Production" else "Add Production",
            onBack = {
                if (isEditMode) viewModel.clearEntryForEdit()
                onBack()
            }
        )

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            // ════════════════════════════════════════════════════════════════
            // EDIT MODE — locked info card + editable quantity fields only
            // ════════════════════════════════════════════════════════════════
            if (isEditMode) {

                // Helper text banner
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(InfoBlue.copy(alpha = 0.12f))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = InfoBlue,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Only quantities can be edited. Worker, product and date are locked to preserve historical records.",
                            color = InfoBlue,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // ── Locked info card ──────────────────────────────────────
                item { SectionHeader(text = "Entry details (read only)") }
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(SurfaceCard)
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        LockedInfoRow(label = "Worker",  value = lockedWorkerName)
                        Box(Modifier.fillMaxWidth().height(0.5.dp).background(OverlayWhite07))
                        LockedInfoRow(label = "Product", value = lockedProductLabel)
                        Box(Modifier.fillMaxWidth().height(0.5.dp).background(OverlayWhite07))
                        LockedInfoRow(label = "Date",    value = lockedDate)
                        Box(Modifier.fillMaxWidth().height(0.5.dp).background(OverlayWhite07))
                        LockedInfoRow(label = "Rate",    value = lockedRate)
                    }
                }

                // ── Editable quantity fields ──────────────────────────────
                item { SectionHeader(text = "Quantities") }
                item {
                    KulhadTextField(
                        label         = "Quantity produced",
                        value         = qty,
                        onValueChange = { qty = it.filter { ch -> ch.isDigit() } },
                        keyboardType  = KeyboardType.Number
                    )
                }
                item {
                    KulhadTextField(
                        label         = "Defective quantity",
                        value         = defective,
                        onValueChange = { defective = it.filter { ch -> ch.isDigit() } },
                        keyboardType  = KeyboardType.Number,
                        helper        = "Defective pieces will not be added to stock"
                    )
                }

                // ── Preview ───────────────────────────────────────────────
                item { EntryPreviewCard(net = net, earnings = earnings) }

                if (qtyInt > 0 && defInt > qtyInt) {
                    item {
                        Text("Defective cannot exceed quantity", color = ErrorRed, fontSize = 13.sp)
                    }
                }

                item {
                    KulhadButton(
                        text    = "Update Entry",
                        enabled = qtyInt > 0 && defInt <= qtyInt,
                        onClick = {
                            val id = entryId ?: return@KulhadButton
                            viewModel.updateEntry(id, qtyInt, defInt) {
                                viewModel.clearEntryForEdit()
                                onBack()
                            }
                        }
                    )
                }

            // ════════════════════════════════════════════════════════════════
            // ADD MODE — original full form (unchanged)
            // ════════════════════════════════════════════════════════════════
            } else {

                // Worker selection
                item { SectionHeader(text = "Worker") }
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(SurfaceCard)
                            .padding(horizontal = 12.dp)
                    ) {
                        activeWorkers.forEachIndexed { idx, w ->
                            val sel = w.id == workerId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { workerId = w.id }
                                    .padding(vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (sel) PrimaryBlue else Color.Transparent)
                                )
                                Text(
                                    text       = w.name,
                                    color      = TextPrimary,
                                    fontSize   = 17.sp,
                                    fontWeight = if (sel) FontWeight.W600 else FontWeight.W500,
                                    modifier   = Modifier.weight(1f)
                                )
                                Text(
                                    text     = w.currentType.name.lowercase().replaceFirstChar { it.uppercase() },
                                    color    = if (sel) PrimaryBlue else TextSecondary,
                                    fontSize = 17.sp
                                )
                            }
                            if (idx < activeWorkers.lastIndex) {
                                Box(Modifier.fillMaxWidth().height(0.5.dp).background(OverlayWhite07))
                            }
                        }
                        if (activeWorkers.isEmpty()) {
                            Text(
                                "No workers found",
                                color = TextSecondary, fontSize = 13.sp,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        }
                    }
                }

                // Kulhad size picker
                item { SectionHeader(text = "Kulhad size") }
                item {
                    SizePillGrid(
                        sizes    = products.map { it.product.sizeMl },
                        labels   = products.associate { it.product.sizeMl to it.product.displayLabel },
                        selected = sizeMl,
                        onSelect = { sizeMl = it }
                    )
                }
                if (sizeMl != null) {
                    item {
                        Text(
                            text  = "Rate · ${sizeMl}ml — ${Money.formatRupeesDouble(rate)} / piece",
                            color = WarningAmber,
                            fontSize = 13.sp
                        )
                    }
                }

                item {
                    KulhadTextField(
                        label         = "Quantity produced",
                        value         = qty,
                        onValueChange = { qty = it.filter { ch -> ch.isDigit() } },
                        keyboardType  = KeyboardType.Number
                    )
                }
                item {
                    KulhadTextField(
                        label         = "Defective quantity",
                        value         = defective,
                        onValueChange = { defective = it.filter { ch -> ch.isDigit() } },
                        keyboardType  = KeyboardType.Number,
                        helper        = "Defective pieces will not be added to stock"
                    )
                }

                item {
                    WorkingDateChip(
                        selectedDate   = workingDate,
                        onDateSelected = { viewModel.setWorkingDate(it) }
                    )
                }

                item { EntryPreviewCard(net = net, earnings = earnings) }

                if (qtyInt > 0 && defInt > qtyInt) {
                    item {
                        Text("Defective cannot exceed quantity", color = ErrorRed, fontSize = 13.sp)
                    }
                }

                item {
                    KulhadButton(
                        text    = "Save Entry",
                        enabled = workerId != null && sizeMl != null && qtyInt > 0 && defInt <= qtyInt,
                        onClick = {
                            val pid = products.firstOrNull { it.product.sizeMl == sizeMl }?.product?.id
                                ?: return@KulhadButton
                            val wid = workerId ?: return@KulhadButton
                            viewModel.saveEntry(wid, pid, qtyInt, defInt) { _ -> onBack() }
                        }
                    )
                }
            }
        }
    }
}

// ── Private composables ───────────────────────────────────────────────────────

@Composable
private fun LockedInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 13.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text     = label,
            color    = TextSecondary,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text       = value,
                color      = TextPrimary,
                fontSize   = 14.sp,
                fontWeight = FontWeight.W500
            )
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null,
                tint = TextTertiary,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

@Composable
private fun EntryPreviewCard(net: Int, earnings: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceCard)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Column {
            Text("Stock change", color = TextSecondary, fontSize = 17.sp, letterSpacing = 0.5.sp)
            Text("+$net pcs",   color = Success,       fontSize = 17.sp, fontWeight = FontWeight.W600)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("Earnings",                        color = TextSecondary, fontSize = 17.sp, letterSpacing = 0.5.sp)
            Text(Money.formatRupeesDouble(earnings), color = Success,      fontSize = 17.sp, fontWeight = FontWeight.W600)
        }
    }
}
