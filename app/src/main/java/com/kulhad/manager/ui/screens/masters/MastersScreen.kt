package com.kulhad.manager.ui.screens.masters

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Inventory
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kulhad.manager.ui.components.KulhadTopBar
import com.kulhad.manager.ui.theme.BgDeep
import com.kulhad.manager.ui.theme.InfoBlue
import com.kulhad.manager.ui.theme.OverlayWhite07
import com.kulhad.manager.ui.theme.SurfaceCard
import com.kulhad.manager.ui.theme.TextPrimary
import com.kulhad.manager.ui.theme.TextSecondary

/**
 * Masters hub screen — entry point for all master-data management sections.
 *
 * Phase 1 exposes only the Product Master.  Future phases will add:
 *  - Production Rates
 *  - Settings
 *  - User Management
 * Each new section is added as another [MasterItem] row plus a new route and screen.
 */
@Composable
fun MastersScreen(
    onBack: () -> Unit,
    onProductMaster: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(BgDeep)) {
        KulhadTopBar(title = "Masters", onBack = onBack)
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceCard)
                        .padding(horizontal = 12.dp)
                ) {
                    MasterItem(
                        label       = "Products / Kulhad Sizes",
                        subtitle    = "Add, edit and reorder kulhad sizes",
                        icon        = Icons.Outlined.Inventory,
                        iconTint    = InfoBlue,
                        showDivider = false,
                        onClick     = onProductMaster
                    )
                    // Future items added here as additional MasterItem rows with showDivider = true
                }
            }
        }
    }
}

@Composable
private fun MasterItem(
    label: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    showDivider: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = null,
            tint               = iconTint,
            modifier           = Modifier.size(22.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label,    color = TextPrimary,   fontSize = 15.sp, fontWeight = FontWeight.W500)
            Text(text = subtitle, color = TextSecondary, fontSize = 12.sp)
        }
        Icon(
            imageVector        = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint               = TextSecondary,
            modifier           = Modifier.size(18.dp)
        )
    }
    if (showDivider) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(OverlayWhite07)
        )
    }
}
