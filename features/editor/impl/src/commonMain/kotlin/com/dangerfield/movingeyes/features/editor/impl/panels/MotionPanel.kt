@file:Suppress("MagicNumber")

package com.dangerfield.movingeyes.features.editor.impl.panels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dangerfield.movingeyes.features.editor.impl.EditorState
import com.dangerfield.movingeyes.features.editor.impl.label
import com.dangerfield.movingeyes.features.paywall.PaywallTrigger
import com.dangerfield.movingeyes.libraries.eyes.BehaviorConfig
import com.dangerfield.movingeyes.libraries.eyes.Mood
import com.dangerfield.movingeyes.libraries.ui.components.Switch
import com.dangerfield.movingeyes.libraries.ui.components.chip.SelectChip
import com.dangerfield.movingeyes.libraries.ui.components.text.Text
import com.dangerfield.movingeyes.system.AppTheme
import com.dangerfield.movingeyes.system.Dimension
import movingeyes.libraries.resources.generated.resources.Res
import movingeyes.libraries.resources.generated.resources.motion_blink_rate
import movingeyes.libraries.resources.generated.resources.motion_free_caption
import movingeyes.libraries.resources.generated.resources.motion_mood
import movingeyes.libraries.resources.generated.resources.motion_reactivity
import movingeyes.libraries.resources.generated.resources.motion_restlessness
import movingeyes.libraries.resources.generated.resources.motion_wander
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

/**
 * The whole paywall, in one tab: the paid line runs along *how eyes move*, not
 * how many there are. Nothing is disabled — a locked tap runs a demo, or opens
 * the paywall once that control's demo is spent.
 */
@Composable
fun MotionPanel(
    editor: EditorState,
    isUnlocked: Boolean,
    onMoodPicked: (Mood) -> Unit,
    onReactivityChange: (Boolean) -> Unit,
    onLocked: (PaywallTrigger) -> Unit,
    modifier: Modifier = Modifier,
) {
    val behavior = editor.activeBehavior()
    val activeMood = editor.activeMood()

    // Null when unlocked, which is what makes the sliders live. Locked, they
    // are inert with a single tap target: a slider you can drag but that only
    // ever opens a paywall fired one navigation per frame of the drag, and
    // stacked thirty paywalls to back out of.
    val lockedMotion = if (isUnlocked) null else ({ onLocked(PaywallTrigger.Motion) })


    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimension.D600),
    ) {
        PanelRow(
            label = stringResource(Res.string.motion_mood),
            onLocked = lockedMotion,
        ) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(Dimension.D300)) {
                items(SelectableMoods.size) { index ->
                    val mood = SelectableMoods[index]
                    SelectChip(
                        label = stringResource(mood.label),
                        selected = mood == activeMood,
                        onClick = { onMoodPicked(mood) },
                    )
                }
            }
        }

        PanelSlider(
            label = stringResource(Res.string.motion_blink_rate),
            value = blinksPerMinute(behavior.blinkIntervalSeconds.midpoint()),
            valueLabel = "${blinksPerMinute(behavior.blinkIntervalSeconds.midpoint()).roundToInt()}/min",
            onValueChange = { rate ->
                    editor.setBehavior(behavior.withBlinksPerMinute(rate))
            },
            valueRange = MinBlinksPerMinute..MaxBlinksPerMinute,
            onLocked = lockedMotion,
        )

        PanelSlider(
            label = stringResource(Res.string.motion_wander),
            value = behavior.gazeRange,
            valueLabel = "${(behavior.gazeRange * 100).roundToInt()}%",
            onValueChange = { range ->
                    editor.setBehavior(behavior.copy(gazeRange = range))
            },
            onLocked = lockedMotion,
        )

        PanelSlider(
            label = stringResource(Res.string.motion_restlessness),
            value = behavior.saccadeSpeed / MaxSaccadeSpeed,
            valueLabel = "${(behavior.saccadeSpeed / MaxSaccadeSpeed * 100).roundToInt()}%",
            onValueChange = { amount ->
                    editor.setBehavior(behavior.withRestlessness(amount))
            },
            onLocked = lockedMotion,
        )

        PanelRow(
            label = stringResource(Res.string.motion_reactivity),
            isLocked = !isUnlocked,
            onLocked = { onLocked(PaywallTrigger.Reactivity) },
            trailing = {
                Switch(
                    checked = editor.canvas.reactivityEnabled,
                    onCheckedChange = onReactivityChange,
                    enabled = isUnlocked,
                )
            },
        ) {}

        if (!isUnlocked) {
            Text(
                text = stringResource(Res.string.motion_free_caption),
                typography = AppTheme.typography.Caption.C300,
                color = AppTheme.colors.textTertiary,
            )
        }
    }
}

/** Custom is a state the sliders put you in, never something you pick. */
private val SelectableMoods = Mood.entries.filter { it != Mood.Custom }

private fun ClosedFloatingPointRange<Float>.midpoint(): Float = (start + endInclusive) / 2f

private fun blinksPerMinute(intervalSeconds: Float): Float =
    if (intervalSeconds <= 0f) MaxBlinksPerMinute else SecondsPerMinute / intervalSeconds

/**
 * The engine needs an interval *range* — a fixed interval reads as a
 * screensaver — so the slider sets a midpoint and the spread is rebuilt.
 */
private fun BehaviorConfig.withBlinksPerMinute(rate: Float) = copy(
    blinkIntervalSeconds = (SecondsPerMinute / rate.coerceAtLeast(0.1f)).let { midpoint ->
        (midpoint * (1f - BlinkSpread))..(midpoint * (1f + BlinkSpread))
    },
)

/** One slider driving saccade speed, interval and jitter together: nobody
 *  adjusts nine sliders at 6pm on the 31st. */
private fun BehaviorConfig.withRestlessness(amount: Float): BehaviorConfig {
    val level = amount.coerceIn(0f, 1f)
    val midpoint = MaxSaccadeIntervalSeconds - (MaxSaccadeIntervalSeconds - MinSaccadeIntervalSeconds) * level
    return copy(
        saccadeSpeed = (level * MaxSaccadeSpeed).coerceAtLeast(0.1f),
        saccadeIntervalSeconds = (midpoint * (1f - SaccadeSpread))..(midpoint * (1f + SaccadeSpread)),
        jitter = MinJitter + (MaxJitter - MinJitter) * level,
    )
}

private const val SecondsPerMinute = 60f
private const val MinBlinksPerMinute = 2f
private const val MaxBlinksPerMinute = 60f

/** Fraction either side of the midpoint, wide enough that two eyes drift
 *  apart rather than marching. */
private const val BlinkSpread = 0.45f
private const val SaccadeSpread = 0.55f

private const val MaxSaccadeSpeed = 3f
private const val MinSaccadeIntervalSeconds = 0.3f
private const val MaxSaccadeIntervalSeconds = 8f
private const val MinJitter = 0.005f
private const val MaxJitter = 0.09f
