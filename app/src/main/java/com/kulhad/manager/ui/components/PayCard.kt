package com.kulhad.manager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kulhad.manager.ui.theme.Success
import com.kulhad.manager.ui.theme.SurfaceCard
import com.kulhad.manager.ui.theme.TextPrimary
import com.kulhad.manager.ui.theme.TextSecondary

/**
 * Pay/salary card matching HTML screen 5 (Payroll).
 * Shows worker avatar, name, computation detail, and net earning.
 */
@Composable
fun PayCard(
    name: String,
    detail: String,
    amount: String,
    modifier: Modifier = Modifier,
    amountColor: androidx.compose.ui.graphics.Color = Success
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceCard)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        WorkerAvatar(name = name, size = 32.dp, fontSize = 10)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = name, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.W500)
            Text(text = detail, color = TextSecondary, fontSize = 10.sp)
        }
        Text(text = amount, color = amountColor, fontSize = 13.sp, fontWeight = FontWeight.W600)
    }
}

/**
 * Compact icon-box sale row matching HTML screen 7 (Sales).
 * Shows a colored icon box on the left, customer name + meta, amount on right.
 */
@Composable
fun SaleRowItem(
    customerName: String,
    meta: String,
    amount: String,
    modifier: Modifier = Modifier,
    amountColor: androidx.compose.ui.graphics.Color = Success,
    trailingContent: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Icon box
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .padding(0.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(androidx.compose.ui.graphics.Color(0xFF1E3A5F))
                .padding(6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "🏭", fontSize = 12.sp)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(text = customerName, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.W500)
            Text(text = meta, color = TextSecondary, fontSize = 9.sp)
        }
        if (trailingContent != null) {
            trailingContent()
        } else {
            Text(text = amount, color = amountColor, fontSize = 12.sp, fontWeight = FontWeight.W600)
        }
    }
}
