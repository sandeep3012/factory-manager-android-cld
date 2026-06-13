package com.kulhad.manager.ui.screens.customers

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kulhad.manager.domain.model.CUSTOMER_TYPES
import com.kulhad.manager.ui.components.KulhadButton
import com.kulhad.manager.ui.components.KulhadTextField
import com.kulhad.manager.ui.components.KulhadTopBar
import com.kulhad.manager.ui.components.SectionHeader
import com.kulhad.manager.ui.theme.BgDeep
import com.kulhad.manager.ui.theme.BorderLine
import com.kulhad.manager.ui.theme.PrimaryBlue
import com.kulhad.manager.ui.theme.PrimaryBlueLight
import com.kulhad.manager.ui.theme.SurfaceCard
import com.kulhad.manager.ui.theme.TextPrimary
import com.kulhad.manager.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddCustomerScreen(
    onBack: () -> Unit,
    editCustomerId: Long? = null,
    viewModel: CustomerViewModel = hiltViewModel()
) {
    val isEdit = editCustomerId != null
    val editCustomer by viewModel.editCustomer.collectAsStateWithLifecycle()

    var name         by remember { mutableStateOf("") }
    var phone        by remember { mutableStateOf("") }
    var address      by remember { mutableStateOf("") }
    var customerType by remember { mutableStateOf(CUSTOMER_TYPES.first()) }

    LaunchedEffect(editCustomerId) {
        if (editCustomerId != null) viewModel.loadCustomerForEdit(editCustomerId)
    }

    LaunchedEffect(editCustomer) {
        editCustomer?.let { c ->
            name         = c.name
            phone        = c.mobileNumber
            address      = c.address
            customerType = c.customerType
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(BgDeep)) {
        KulhadTopBar(
            title = if (isEdit) "Edit Customer" else "Add Customer",
            onBack = {
                viewModel.clearEditCustomer()
                onBack()
            }
        )
        LazyColumn(
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Customer code — read-only in edit mode
            if (isEdit && editCustomer != null) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceCard)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(text = "Customer Code", color = TextSecondary, fontSize = 12.sp)
                        Text(
                            text = editCustomer!!.customerCode,
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.W500
                        )
                    }
                }
            }

            item {
                KulhadTextField(
                    label = "Customer name *",
                    value = name,
                    onValueChange = { name = it }
                )
            }
            item {
                KulhadTextField(
                    label = "Mobile number",
                    value = phone,
                    onValueChange = { phone = it.filter { ch -> ch.isDigit() || ch == '+' } },
                    keyboardType = KeyboardType.Phone
                )
            }
            item {
                KulhadTextField(
                    label = "Address",
                    value = address,
                    onValueChange = { address = it }
                )
            }

            // Customer Type chips
            item { SectionHeader(text = "Customer Type") }
            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CUSTOMER_TYPES.forEach { type ->
                        val selected = type == customerType
                        Text(
                            text = type,
                            color = if (selected) PrimaryBlue else TextSecondary,
                            fontSize = 14.sp,
                            fontWeight = if (selected) FontWeight.W600 else FontWeight.W400,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (selected) PrimaryBlueLight.copy(alpha = 0.15f) else SurfaceCard)
                                .border(
                                    width = 1.dp,
                                    color = if (selected) PrimaryBlue else BorderLine,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable { customerType = type }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            item {
                KulhadButton(
                    text = if (isEdit) "Update Customer" else "Save Customer",
                    enabled = name.isNotBlank(),
                    onClick = {
                        viewModel.saveCustomer(
                            name = name,
                            phone = phone,
                            address = address,
                            customerType = customerType,
                            editId = editCustomerId
                        ) {
                            viewModel.clearEditCustomer()
                            onBack()
                        }
                    }
                )
            }
        }
    }
}
