package com.kulhad.manager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import com.kulhad.manager.ui.theme.PrimaryBlueDark
import com.kulhad.manager.ui.theme.PrimaryBlueLight
import com.kulhad.manager.ui.theme.SurfaceCard
import com.kulhad.manager.ui.theme.TextTertiary

/**
 * Grid of pill chips for selecting a kulhad size.
 *
 * [sizes] is the list of sizeMl values used as keys.
 * [labels] is an optional map of sizeMl → display label (e.g. "80ml", "Half Litre").
 * Falls back to "${size}ml" when a label is missing from the map.
 */
@Composable
fun SizePillGrid(
    sizes: List<Int>,
    selected: Int?,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    columns: Int = 4,
    labels: Map<Int, String> = emptyMap()
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        sizes.chunked(columns).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { size ->
                    val isSelected = selected == size
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) PrimaryBlueDark else SurfaceCard)
                            .clickable { onSelect(size) }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = labels[size] ?: "${size}ml",
                            color = if (isSelected) PrimaryBlueLight else TextTertiary,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.W600 else FontWeight.W500
                        )
                    }
                }
                // Pad the row out if shorter
                repeat(columns - row.size) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
