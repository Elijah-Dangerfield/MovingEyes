package com.dangerfield.movingeyes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.dangerfield.movingeyes.libraries.eyes.EyeStyles
import com.dangerfield.movingeyes.libraries.eyes.EyeTier
import com.dangerfield.movingeyes.libraries.eyes.Mood
import com.dangerfield.movingeyes.libraries.eyes.Moods
import com.dangerfield.movingeyes.libraries.render.EyePreview
import com.dangerfield.movingeyes.libraries.ui.components.LockBadge
import com.dangerfield.movingeyes.libraries.ui.components.SegmentedControl
import com.dangerfield.movingeyes.libraries.ui.components.text.Text
import com.dangerfield.movingeyes.system.AppTheme
import com.dangerfield.movingeyes.system.Dimension

/**
 * All twelve styles, animating, with a mood switch.
 *
 * A style is a set of numbers, and numbers can only be judged on a real screen
 * in a real room — a gradient stop that looks right on a monitor can turn an
 * iris into a flat disc on an OLED tablet at arm's length. This is where that
 * gets checked, and where a new style gets tuned.
 *
 * The mood switch is here because motion is most of what distinguishes these:
 * Ghoul on Dormant and Ghoul on Possessed are different products.
 */
@Composable
fun EyeGalleryScreen(modifier: Modifier = Modifier) {
    var mood by remember { mutableStateOf(Mood.IdleScan) }
    val behavior = remember(mood) { Moods.forMood(mood) }

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = Dimension.D800),
        verticalArrangement = Arrangement.spacedBy(Dimension.D700),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Dimension.D300)) {
            Text(text = "Eyes", typography = AppTheme.typography.Display.D1000)
            Text(
                text = "Every style is a parameter set, not an image. Judge them here, on a " +
                    "screen, in the light you'll actually use them in.",
                typography = AppTheme.typography.Body.B400,
                color = AppTheme.colors.textSecondary,
            )
        }

        // Four at a time so each segment clears the touch minimum.
        SegmentedControl(
            options = listOf(Mood.IdleScan, Mood.Suspicious, Mood.Frantic, Mood.Dormant),
            selected = mood,
            onSelect = { mood = it },
            label = { it.label() },
        )
        SegmentedControl(
            options = listOf(Mood.Sleepy, Mood.Possessed),
            selected = mood,
            onSelect = { mood = it },
            label = { it.label() },
        )

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 120.dp),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(Dimension.D500),
            verticalArrangement = Arrangement.spacedBy(Dimension.D500),
        ) {
            items(EyeStyles.All, key = { it.id }) { style ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(Dimension.D300),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            // True black, matching the canvas — a style has to
                            // be judged against the surface it will sit on.
                            .background(AppTheme.colors.background.color)
                            .border(1.dp, AppTheme.colors.border.color, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        EyePreview(
                            style = style,
                            modifier = Modifier.fillMaxSize().padding(Dimension.D700),
                            sizeDp = 84.dp,
                            behavior = behavior,
                            canvasColor = AppTheme.colors.background.color,
                        )
                        if (style.tier == EyeTier.Paid) {
                            LockBadge(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(Dimension.D200),
                            )
                        }
                    }
                    Text(
                        text = style.displayName,
                        typography = AppTheme.typography.Label.L400,
                        color = AppTheme.colors.textSecondary,
                    )
                }
            }
        }
    }
}

private fun Mood.label(): String = when (this) {
    Mood.IdleScan -> "Idle"
    Mood.Suspicious -> "Suspicious"
    Mood.Frantic -> "Frantic"
    Mood.Sleepy -> "Sleepy"
    Mood.Dormant -> "Dormant"
    Mood.Possessed -> "Possessed"
    Mood.Custom -> "Custom"
}
