package com.kulhad.manager.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Navigation-bar and IME padding for bottom sheet root content in edge-to-edge mode.
 *
 * Use with [Modifier.padding] on the root [Column] inside
 * [androidx.compose.material3.ModalBottomSheet].
 */
@Composable
fun bottomSheetContentPadding(): PaddingValues =
    WindowInsets.navigationBars.union(WindowInsets.ime).asPaddingValues()

/**
 * Applies [bottomSheetContentPadding] to bottom sheet root content.
 */
@Composable
fun Modifier.bottomSheetContentInsets(): Modifier =
    padding(bottomSheetContentPadding())
