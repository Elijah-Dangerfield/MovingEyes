package com.dangerfield.movingeyes.features.editor.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * The canvas is the screen at 1:1 — no zoom, no pan, no insets. Black bleeds
 * to every edge, including under the notch, so on OLED the lit pixels are the
 * eyes and nothing else.
 *
 * Phase 0 is the empty canvas. The renderer lands in Phase 3.
 */
@Composable
fun EditorScreen(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().background(Color.Black))
}
