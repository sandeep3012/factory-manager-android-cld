package com.kulhad.manager.ui.screens.customers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kulhad.manager.data.util.Money
import com.kulhad.manager.domain.model.CustomerWithStats
import com.kulhad.manager.ui.components.BadgeType
import com.kulhad.manager.ui.components.EmptyState
import com.kulhad.manager.ui.components.KulhadTextField
import com.kulhad.manager.ui.components.KulhadTopBar
import com.kulhad.manager.ui.components.StatusBadge
import com.kulhad.manager.ui.theme.BgDeep
import com.kulhad.manager.ui.theme.BorderLine
import com.kulhad.manager.ui.theme.PrimaryBlue
import com.kulhad.manager.ui.theme.SurfaceCard
import com.kulhad.manager.ui.theme.TextPrimary
import com.kulhad.manager.ui.theme.TextSecondary
import com.kulhad.manager.ui.theme.TextTertiary
import com.kulhad.manager.ui.theme.WarningAmber

@Composable
fun CustomerListScreen(
    onBack: () -> Unit,
    onAddCustomer: () -> Unit,
    onCustomerClick: (Long) -> Unit,
    viewModel: CustomerViewModel = hiltViewModel()
) {
    val customers by viewModel.customerList.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = BgDeep,
        topBar = { KulhadTopBar(title = "Customers", onBack = onBack) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddCustomer,
                containerColor = PrimaryBlue,
                shape = CircleShape
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Add customer", tint = TextPrimary)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            KulhadTextField(
                label = "Search by name or phone",
                value = searchQuery,
                onValueChange = { viewModel.setSearch(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            )

            if (customers.isEmpty()) {
                EmptyState(
                    message = if (searchQuery.isBlank()) "No customers yet" else "No results for \"$searchQuery\""
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(customers, key = { it.customer.id }) { item ->
                        CustomerCard(item = item, onClick = { onCustomerClick(item.customer.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerCard(item: CustomerWithStats, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceCard)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Avatar
        Row(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(BorderLine),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.Person,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(24.dp)
            )
        }

        // Name, code, phone, type
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = item.customer.name, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.W500)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = item.customer.customerCode, color = TextTertiary, fontSize = 12.sp)
                Text(text = "·", color = TextTertiary, fontSize = 12.sp)
                Text(text = item.customer.customerType, color = TextTertiary, fontSize = 12.sp)
            }
            if (item.customer.mobileNumber.isNotBlank()) {
                Text(text = item.customer.mobileNumber, color = TextSecondary, fontSize = 13.sp)
            }
        }

        // Outstanding / total
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (item.outstanding > 0) {
                StatusBadge(text = "Due", type = BadgeType.ERROR)
                Text(
                    text = Money.formatRupees(item.outstanding),
                    color = WarningAmber,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.W500
                )
            } else if (item.totalSales > 0) {
                StatusBadge(text = "Clear", type = BadgeType.SUCCESS)
            }
        }
    }
}
