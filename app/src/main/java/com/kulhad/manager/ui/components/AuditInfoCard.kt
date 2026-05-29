package com.kulhad.manager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kulhad.manager.data.util.DateUtils
import com.kulhad.manager.domain.model.AuditDisplay
import com.kulhad.manager.ui.theme.BgDeep
import com.kulhad.manager.ui.theme.OverlayWhite07
import com.kulhad.manager.ui.theme.TextPrimary
import com.kulhad.manager.ui.theme.TextSecondary
import com.kulhad.manager.ui.theme.TextTertiary

/**
 * Read-only card that surfaces [AuditDisplay] metadata on detail / history dialogs.
 *
 * Two sections are always rendered:
 *  - **CREATED** — createdBy + createdAt. If [AuditDisplay.createdAt] == 0L the timestamp
 *    is shown as "—" to indicate a row that predates audit tracking (migrated from v1 schema).
 *  - **LAST UPDATED** — updatedBy + updatedAt when both are non-null, otherwise the italic
 *    placeholder "Never Updated".
 *
 * The card uses [BgDeep] as its background so it reads as a visually distinct inset when
 * the parent dialog uses [com.kulhad.manager.ui.theme.SurfaceCard].
 *
 * Reusable across attendance, sale details, payment details, production details, advance details.
 * Obtain [AuditDisplay] via the [com.kulhad.manager.data.util.toDisplay] extension on [AuditInfo].
 */
@Composable
fun AuditInfoCard(
    audit: AuditDisplay,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(BgDeep)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // ── CREATED ──────────────────────────────────────────────────────────
        AuditSection(
            label = "CREATED",
            by    = audit.createdBy,
            at    = if (audit.createdAt == 0L) "—" else DateUtils.formatAuditTimestamp(audit.createdAt)
        )

        // Divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(OverlayWhite07)
        )

        // ── LAST UPDATED ─────────────────────────────────────────────────────
        if (audit.updatedBy != null && audit.updatedAt != null) {
            AuditSection(
                label = "LAST UPDATED",
                by    = audit.updatedBy,
                at    = if (audit.updatedAt == 0L) "—" else DateUtils.formatAuditTimestamp(audit.updatedAt)
            )
        } else {
            Text(
                text      = "Never Updated",
                color     = TextTertiary,
                fontSize  = 11.sp,
                fontStyle = FontStyle.Italic
            )
        }
    }
}

// ── Internal helpers ─────────────────────────────────────────────────────────

@Composable
private fun AuditSection(
    label: String,
    by: String,
    at: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text          = label,
            color         = TextTertiary,
            fontSize      = 10.sp,
            fontWeight    = FontWeight.W600,
            letterSpacing = 0.8.sp
        )
        AuditFieldRow(fieldLabel = "By", value = by)
        AuditFieldRow(fieldLabel = "At", value = at)
    }
}

@Composable
private fun AuditFieldRow(fieldLabel: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text     = "$fieldLabel:",
            color    = TextSecondary,
            fontSize = 12.sp
        )
        Text(
            text       = value,
            color      = TextPrimary,
            fontSize   = 12.sp,
            fontWeight = FontWeight.W500
        )
    }
}
