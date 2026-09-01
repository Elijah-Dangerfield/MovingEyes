@file:Suppress("MagicNumber")

package com.dangerfield.movingeyes.features.editor.impl

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dangerfield.movingeyes.libraries.device.BatteryState
import com.dangerfield.movingeyes.libraries.ui.components.button.Button
import com.dangerfield.movingeyes.libraries.ui.components.button.ButtonSize
import com.dangerfield.movingeyes.libraries.ui.components.text.Text
import com.dangerfield.movingeyes.system.AppTheme
import com.dangerfield.movingeyes.system.Dimension
import com.dangerfield.movingeyes.system.Motion
import movingeyes.libraries.resources.generated.resources.Res
import movingeyes.libraries.resources.generated.resources.display_battery
import movingeyes.libraries.resources.generated.resources.display_hint_body
import movingeyes.libraries.resources.generated.resources.display_hint_got_it
import movingeyes.libraries.resources.generated.resources.display_hint_title
import org.jetbrains.compose.resources.stringResource

/**
 * Everything that may appear over a running scene.
 *
 * The shared rule for all of it: **never over the eyes, never blocking, never
 * demanding a tap.** Someone is looking at this from across a room, or nobody
 * is looking at it at all. Anything that waits for acknowledgement is a bright
 * rectangle sitting in a cardboard cut-out until the owner comes home.
 */
@Composable
fun BoxScope.DisplayOverlay(
    state: DisplayModeState,
    showHint: Boolean,
    onHintAcknowledged: () -> Unit,
) {
    BatteryPill(
        battery = state.batteryNotice,
        modifier = Modifier
            .align(Alignment.TopEnd)
            .safeDrawingPadding()
            .padding(Dimension.D700),
    )

    if (showHint) {
        DisplayHintCard(
            onAcknowledged = onHintAcknowledged,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .safeDrawingPadding()
                .padding(Dimension.D700),
        )
    }
}

/**
 * Charge level, top-right, out of the way of a face in the middle of the
 * canvas. Amber once it's low, because at that point it has stopped being
 * information and started being a warning.
 */
@Composable
private fun BatteryPill(battery: BatteryState?, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = battery != null,
        modifier = modifier,
        enter = fadeIn(Motion.Chrome.restore()),
        exit = fadeOut(Motion.Chrome.dissolve()),
    ) {
        val percent = battery?.percent ?: return@AnimatedVisibility
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(percent = 50))
                .background(AppTheme.colors.surfacePrimary.color.copy(alpha = 0.85f))
                .padding(horizontal = Dimension.D500, vertical = Dimension.D300),
        ) {
            Text(
                text = stringResource(Res.string.display_battery, percent),
                typography = AppTheme.typography.Readout.R300,
                color = if (battery.isLow) {
                    AppTheme.colors.accentPrimary
                } else {
                    AppTheme.colors.textSecondary
                },
            )
        }
    }
}

/**
 * Shown once, ever, on the first entry into display mode.
 *
 * It exists because v2 cut the long-press-corner exit: a glowing ring can land
 * inside a cut hole, and hunting for an invisible corner in the dark is worse
 * than being told once. The system back gesture is the way out, and nobody
 * guesses that on their own when the screen has stopped responding to taps.
 */
@Composable
private fun DisplayHintCard(onAcknowledged: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .widthIn(max = HintMaxWidth)
            .clip(RoundedCornerShape(HintCornerRadius))
            .background(AppTheme.colors.surfacePrimary.color)
            .padding(Dimension.D700),
        verticalArrangement = Arrangement.spacedBy(Dimension.D400),
    ) {
        Text(
            text = stringResource(Res.string.display_hint_title),
            typography = AppTheme.typography.Heading.H600,
        )
        Text(
            text = stringResource(Res.string.display_hint_body),
            typography = AppTheme.typography.Body.B600,
            color = AppTheme.colors.textSecondary,
        )
        Button(
            onClick = onAcknowledged,
            size = ButtonSize.Small,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(Res.string.display_hint_got_it))
        }
    }
}

/**
 * The sleep-timer fade, drawn over everything including the overlay.
 *
 * Black rather than a brightness change, so it works identically on both
 * platforms and reaches genuine zero — the OS backlight floor is still visible
 * in a dark hallway, which is the whole reason software dimming exists here.
 */
@Composable
fun BoxScope.SleepFade(fade: Float) {
    if (fade >= 1f) return
    Box(
        modifier = Modifier
            .matchParentSize()
            .background(Color.Black.copy(alpha = 1f - fade)),
    )
}

private val HintMaxWidth = 420.dp
private val HintCornerRadius = 14.dp
