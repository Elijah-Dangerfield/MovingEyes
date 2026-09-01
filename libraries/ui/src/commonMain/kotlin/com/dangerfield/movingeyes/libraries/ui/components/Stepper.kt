package com.dangerfield.movingeyes.libraries.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.dangerfield.movingeyes.libraries.ui.PreviewContent
import com.dangerfield.movingeyes.libraries.ui.components.text.Text
import com.dangerfield.movingeyes.system.AppTheme
import com.dangerfield.movingeyes.system.Dimension
import com.dangerfield.movingeyes.system.Target
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.compose.ui.tooling.preview.Preview

/** How long a press is held before it starts repeating. */
private const val HoldDelayMillis = 350L

/** Interval between repeats once held. */
private const val RepeatIntervalMillis = 60L

/** Steps per repeat tick while held. A tap is 1, a held tick is 10. */
private const val HeldStepMultiplier = 10

private val StepperCornerRadius = 12.dp

/**
 * Two keys that move a value by one unit per tap, ten per tick while held.
 *
 * This is what's left of the design's nine-key nudge pad, and it survives for
 * one reason: dragging is how you move an eye, but the last two millimetres of
 * a cardboard alignment are not a drag problem — a finger covers the thing it
 * is positioning. Snapping does most of that work now, so what's left is a
 * two-key row on the transform strip rather than a panel of its own.
 *
 * A haptic fires on every step so someone holding the device against a wall can
 * count without looking. The design cut the haptics *setting* (a switch with no
 * reason to exist), not the haptics.
 *
 * If this ever feels like dead weight, deleting it costs nothing — arrow keys on
 * a paired keyboard already do the same job.
 */
@Composable
fun Stepper(
    onStep: (Int) -> Unit,
    modifier: Modifier = Modifier,
    unitLabel: String = "1 px",
    holdLabel: String = "hold 10",
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Dimension.D200),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Dimension.D200)) {
            StepKey(label = "−", onStep = { onStep(-it) })
            StepKey(label = "+", onStep = { onStep(it) })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Dimension.D300)) {
            Text(
                text = unitLabel,
                typography = AppTheme.typography.Readout.R300,
                color = AppTheme.colors.textSecondary,
            )
            Text(
                text = holdLabel,
                typography = AppTheme.typography.Readout.R300,
                color = AppTheme.colors.textTertiary,
            )
        }
    }
}

@Composable
private fun StepKey(
    label: String,
    onStep: (Int) -> Unit,
) {
    val step by rememberUpdatedState(onStep)
    val haptics = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .size(Target.Minimum)
            .clip(RoundedCornerShape(StepperCornerRadius))
            .background(AppTheme.colors.surfaceSecondary.color)
            .border(1.dp, AppTheme.colors.border.color, RoundedCornerShape(StepperCornerRadius))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        step(1)
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)

                        // A release inside the hold delay is a plain tap and
                        // has already emitted its single step. Past it, repeat
                        // at ten a tick until the finger lifts. `null` from
                        // withTimeoutOrNull means "still held".
                        val releasedEarly = withTimeoutOrNull(HoldDelayMillis) { tryAwaitRelease() }
                        if (releasedEarly != null) return@detectTapGestures

                        while (withTimeoutOrNull(RepeatIntervalMillis) { tryAwaitRelease() } == null) {
                            step(HeldStepMultiplier)
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, typography = AppTheme.typography.Heading.H600)
    }
}

@Preview
@Composable
private fun PreviewStepper() {
    PreviewContent(modifier = Modifier.padding(Dimension.D800)) {
        Stepper(onStep = {})
    }
}
