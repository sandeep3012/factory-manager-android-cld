package com.kulhad.manager.ui.screens.expense

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kulhad.manager.ui.components.KulhadButton
import com.kulhad.manager.ui.components.KulhadTextField
import com.kulhad.manager.ui.components.KulhadTopBar
import com.kulhad.manager.ui.components.WorkingDateChip
import com.kulhad.manager.ui.theme.BgDeep
import com.kulhad.manager.ui.theme.OverlayWhite15
import com.kulhad.manager.ui.theme.PrimaryBlueDark
import com.kulhad.manager.ui.theme.PrimaryBlueLight
import com.kulhad.manager.ui.theme.SurfaceCard
import com.kulhad.manager.ui.theme.TextPrimary
import com.kulhad.manager.ui.theme.TextSecondary
import com.kulhad.manager.ui.theme.TextTertiary

@Composable
fun AddExpenseScreen(
    onBack: () -> Unit,
    viewModel: ExpenseViewModel = hiltViewModel()
) {
    val types by viewModel.expenseTypes.collectAsStateWithLifecycle()
    val workingDate by viewModel.workingDate.collectAsStateWithLifecycle()
    var selectedTypeId by remember { mutableStateOf<Long?>(null) }
    var amount by remember { mutableStateOf("") }
    var remark by remember { mutableStateOf("") }
    var newType by remember { mutableStateOf("") }
    var addingType by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(BgDeep)) {
        KulhadTopBar(title = "Add Expense", onBack = onBack)
        LazyColumn(
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { Text("EXPENSE TYPE", color = TextSecondary, fontSize = 13.sp, letterSpacing = 0.5.sp) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    types.chunked(3).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { t ->
                                val sel = selectedTypeId == t.id
                                Text(
                                    text = t.name,
                                    color = if (sel) PrimaryBlueLight else TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = if (sel) FontWeight.W600 else FontWeight.W500,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (sel) PrimaryBlueDark else SurfaceCard)
                                        .clickable { selectedTypeId = t.id }
                                        .padding(vertical = 10.dp, horizontal = 10.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                            repeat(3 - row.size) {
                                Text(text = "", modifier = Modifier.weight(1f))
                            }
                        }
                    }
                    if (addingType) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            KulhadTextField(
                                label = "New type",
                                value = newType,
                                onValueChange = { newType = it },
                                modifier = Modifier.weight(1f)
                            )
                            KulhadButton(
                                text = "Add",
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    if (newType.isNotBlank()) {
                                        viewModel.addExpenseType(newType.trim())
                                        newType = ""
                                        addingType = false
                                    }
                                }
                            )
                        }
                    } else {
                        Text(
                            text = "+ New type",
                            color = TextTertiary,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .border(0.5.dp, OverlayWhite15, RoundedCornerShape(10.dp))
                                .clickable { addingType = true }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
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
            // Working date chip — expense date uses this as business date
            item {
                WorkingDateChip(
                    selectedDate   = workingDate,
                    onDateSelected = { viewModel.setWorkingDate(it) }
                )
            }
            item {
                KulhadButton(
                    text = "Save Expense",
                    enabled = selectedTypeId != null && (amount.toIntOrNull() ?: 0) > 0,
                    onClick = {
                        val tid = selectedTypeId ?: return@KulhadButton
                        val amt = amount.toIntOrNull() ?: return@KulhadButton
                        viewModel.saveExpense(tid, amt, remark) { onBack() }
                    }
                )
            }
        }
    }
}
