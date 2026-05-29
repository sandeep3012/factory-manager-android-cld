package com.kulhad.manager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kulhad.manager.ui.theme.PrimaryBlue
import com.kulhad.manager.ui.theme.SurfaceCard
import com.kulhad.manager.ui.theme.TextPrimary
import com.kulhad.manager.ui.theme.TextSecondary
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

// ── Private helpers ──────────────────────────────────────────────────────────

/** Display format: "15 May 2026" */
private val DATE_FMT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())

/**
 * Converts a [LocalDate] to UTC-midnight epoch millis.
 * Material 3's DatePicker always works in UTC — never in the device timezone —
 * so both directions (LocalDate → picker, picker → LocalDate) must use UTC.
 */
private fun LocalDate.toUtcMillis(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

/** Inverse of [toUtcMillis]: UTC-midnight millis back to [LocalDate]. */
private fun Long.millisToLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

// ── Component ────────────────────────────────────────────────────────────────

/**
 * A tappable full-width chip that displays the current working date and opens a
 * Material 3 [DatePickerDialog] on tap.
 *
 * Restrictions enforced in the picker:
 *  - Future dates are disabled ([SelectableDates.isSelectableDate])
 *  - Future years are hidden    ([SelectableDates.isSelectableYear])
 *
 * A "Today" shortcut in the dismiss area immediately resets to [LocalDate.now]
 * without requiring the user to navigate the calendar.
 *
 * This composable owns NO state beyond the picker-open flag. All working date
 * state lives exclusively in [WorkingDateManager] via the ViewModel.
 *
 * @param selectedDate   Current working date from WorkingDateManager (via ViewModel)
 * @param onDateSelected Callback fired when the user confirms a valid date selection
 * @param modifier       Optional layout modifier
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkingDateChip(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPicker by remember { mutableStateOf(false) }

    // ── Chip row ─────────────────────────────────────────────────────────────
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceCard)
            .border(1.dp, PrimaryBlue.copy(alpha = 0.30f), RoundedCornerShape(10.dp))
            .clickable { showPicker = true }
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.CalendarToday,
            contentDescription = "Working date",
            tint = PrimaryBlue,
            modifier = Modifier.size(15.dp)
        )
        Text(
            text = "Working Date:  ${selectedDate.format(DATE_FMT)}",
            color = TextPrimary,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "Change",
            color = PrimaryBlue,
            fontSize = 12.sp
        )
    }

    // ── Date picker dialog ────────────────────────────────────────────────────
    if (showPicker) {
        // Compute today once so both predicates use the same reference.
        val todayUtc = LocalDate.now().toUtcMillis()

        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.toUtcMillis(),
            selectableDates = object : SelectableDates {
                /** Block strictly-future dates. */
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    utcTimeMillis <= todayUtc

                /** Block years beyond the current one to keep year navigation sensible. */
                override fun isSelectableYear(year: Int): Boolean =
                    year <= LocalDate.now().year
            }
        )

        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis
                            ?.millisToLocalDate()
                            ?.let(onDateSelected)
                        showPicker = false
                    }
                ) {
                    Text("OK", color = PrimaryBlue)
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                    // "Today" shortcut — jumps directly to today without calendar nav
                    TextButton(
                        onClick = {
                            onDateSelected(LocalDate.now())
                            showPicker = false
                        }
                    ) {
                        Text("Today", color = TextSecondary)
                    }
                    TextButton(onClick = { showPicker = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}
