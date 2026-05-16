package com.kulhad.manager.ui.screens.workers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kulhad.manager.data.local.entity.WorkerType
import com.kulhad.manager.data.util.DateUtils
import com.kulhad.manager.ui.components.KulhadButton
import com.kulhad.manager.ui.components.KulhadTextField
import com.kulhad.manager.ui.components.KulhadTopBar
import com.kulhad.manager.ui.components.SegmentedControl
import com.kulhad.manager.ui.theme.BgDeep
import com.kulhad.manager.ui.theme.SurfaceCard
import com.kulhad.manager.ui.theme.TextSecondary

@Composable
fun AddWorkerScreen(
    workerId: Long?,
    onBack: () -> Unit,
    viewModel: WorkerViewModel = hiltViewModel()
) {
    val existing = workerId?.let { viewModel.observeWorker(it).collectAsStateWithLifecycle(null).value }

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var joining by remember { mutableStateOf(DateUtils.todayStart()) }
    var typeStr by remember { mutableStateOf("Piece worker") }
    var dailyRate by remember { mutableStateOf("") }

    LaunchedEffect(existing?.id) {
        existing?.let {
            name = it.name
            phone = it.phone
            address = it.address
            joining = it.joiningDate
            typeStr = if (it.currentType == WorkerType.PIECE) "Piece worker" else "Salary worker"
            dailyRate = if (it.dailyRate > 0) it.dailyRate.toString() else ""
        }
    }

    val type = if (typeStr == "Piece worker") WorkerType.PIECE else WorkerType.SALARY

    Column(modifier = Modifier.fillMaxSize().background(BgDeep)) {
        KulhadTopBar(
            title = if (workerId == null) "Add Worker" else "Edit Worker",
            onBack = onBack
        )
        LazyColumn(
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                KulhadTextField(label = "Full name", value = name, onValueChange = { name = it })
            }
            item {
                KulhadTextField(
                    label = "Phone number",
                    value = phone,
                    onValueChange = { phone = it },
                    keyboardType = KeyboardType.Phone
                )
            }
            item {
                KulhadTextField(label = "Address", value = address, onValueChange = { address = it })
            }
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceCard)
                        .clickable { /* date picker dialog could be added later */ }
                        .padding(12.dp)
                ) {
                    Column {
                        Text(text = "JOINING DATE", color = TextSecondary, fontSize = 10.sp)
                        Text(
                            text = DateUtils.formatDay(joining),
                            color = androidx.compose.ui.graphics.Color.White,
                            fontSize = 17.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
            item {
                SegmentedControl(
                    options = listOf("Piece worker", "Salary worker"),
                    selected = typeStr,
                    onSelect = { typeStr = it }
                )
            }
            if (type == WorkerType.SALARY) {
                item {
                    KulhadTextField(
                        label = "Daily rate (₹/day)",
                        value = dailyRate,
                        onValueChange = { dailyRate = it.filter { ch -> ch.isDigit() } },
                        keyboardType = KeyboardType.Number
                    )
                }
            }
            item {
                KulhadButton(
                    text = if (workerId == null) "Save Worker" else "Update Worker",
                    onClick = {
                        if (name.isBlank() || phone.isBlank() || address.isBlank()) return@KulhadButton
                        viewModel.saveWorker(
                            existingId = workerId,
                            name = name.trim(),
                            phone = phone.trim(),
                            address = address.trim(),
                            joiningDate = joining,
                            type = type,
                            dailyRate = dailyRate.toIntOrNull() ?: 0
                        ) { onBack() }
                    }
                )
            }
        }
    }
}
