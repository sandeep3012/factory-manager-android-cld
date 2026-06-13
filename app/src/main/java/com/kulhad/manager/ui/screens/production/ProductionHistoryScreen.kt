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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.kulhad.manager.ui.components.KpiStrip
import com.kulhad.manager.ui.components.KulhadTopBar
import com.kulhad.manager.ui.components.SectionHeader
import com.kulhad.manager.ui.components.WorkerAvatar
import com.kulhad.manager.ui.components.WorkingDateChip
import com.kulhad.manager.ui.theme.BgDeep
import com.kulhad.manager.ui.theme.OverlayWhite07
import com.kulhad.manager.ui.theme.PurpleAccent
import com.kulhad.manager.ui.theme.Success
import com.kulhad.manager.ui.theme.SurfaceCard
import com.kulhad.manager.ui.theme.TextPrimary
import com.kulhad.manager.ui.theme.TextSecondary
import com.kulhad.manager.ui.theme.WarningAmber

/**
 * Displays all production entries for the globally selected working date.
 *
 * Tapping a row navigates to the Edit Production screen via [onEditEntry].
 */
@Composable
fun ProductionHistoryScreen(
    onBack: () -> Unit,
    onEditEntry: (Long) -> Unit,
    viewModel: ProductionViewModel = hiltViewModel()
) {
    val entries     by viewModel.historyDayEntries.collectAsStateWithLifecycle()
    val workingDate by viewModel.workingDate.collectAsStateWithLifecycle()

    val totalNet       = entries.sumOf { it.netQty }
    val totalDefective = entries.sumOf { it.defectiveQuantity }

    Column(modifier = Modifier.fillMaxSize().background(BgDeep)) {
        KulhadTopBar(title = "Production History", onBack = onBack)

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                WorkingDateChip(
                    selectedDate   = workingDate,
                    onDateSelected = { viewModel.setWorkingDate(it) }
                )
            }

            item {
                KpiStrip(
                    items = listOf(
                        Triple(totalNet.toString(),       "Net pieces", PurpleAccent),
                        Triple(totalDefective.toString(), "Defective",  WarningAmber),
                        Triple(entries.size.toString(),   "Entries",    TextPrimary)
                    )
                )
            }

            if (entries.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text     = "No production entries for this date",
                            color    = TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                item { SectionHeader(text = "Entries — ${entries.size}  ·  tap to edit") }
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(SurfaceCard)
                            .padding(horizontal = 12.dp)
                    ) {
                        entries.forEachIndexed { idx, e ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onEditEntry(e.id) }
                                    .padding(vertical = 10.dp),
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                WorkerAvatar(name = e.workerName, size = 36.dp, fontSize = 11)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text       = e.workerName,
                                        color      = TextPrimary,
                                        fontSize   = 14.sp,
                                        fontWeight = FontWeight.W500
                                    )
                                    Text(
                                        text     = "${e.productLabel}  ·  ${Money.formatRupeesDouble(e.rateSnapshot)}/pc",
                                        color    = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text     = "Net ${e.netQty} • def ${e.defectiveQuantity}",
                                        color    = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                                Text(
                                    text       = Money.formatRupeesDouble(e.earnings),
                                    color      = Success,
                                    fontSize   = 13.sp,
                                    fontWeight = FontWeight.W600
                                )
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
