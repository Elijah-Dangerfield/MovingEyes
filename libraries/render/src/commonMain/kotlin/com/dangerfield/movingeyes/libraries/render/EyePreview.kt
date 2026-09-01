@file:Suppress("MagicNumber")

package com.dangerfield.movingeyes.libraries.render

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dangerfield.movingeyes.libraries.eyes.BehaviorConfig
import com.dangerfield.movingeyes.libraries.eyes.EyeStyle
import com.dangerfield.movingeyes.libraries.eyes.Moods
import kotlin.random.Random

/**
 * One eye, alive, in whatever box you put it in.
 *
 * This is the same renderer the canvas uses, not a simplified stand-in — which
 * is the point. Ten of the twelve styles are paid, and **motion is the thing a
 * screenshot cannot show a friend**, so a locked style has to be visibly
 * blinking and looking around in the picker or the paywall is selling
 * something invisible. It also means a style can never drift from its preview:
 * there is only one implementation.
 *
 * Each preview gets its own [Random], so a grid of twelve doesn't blink in
 * unison — the same rule as the real canvas, for the same reason.
 */
@Composable
fun EyePreview(
    style: EyeStyle,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 56.dp,
    behavior: BehaviorConfig = Moods.FreeDefault,
    canvasColor: Color = Color.Black,
    scleraColor: Color? = null,
    irisColor: Color? = null,
    pupilColor: Color? = null,
    seed: Int = style.id.ordinal,
) {
    val sizePx = with(LocalDensity.current) { sizeDp.toPx() }

    val state = remember(style, behavior, sizePx, scleraColor, irisColor, pupilColor) {
        EyeSceneState(
            eyes = listOf(
                RenderedEye(
                    style = style,
                    centerX = 0.5f,
                    centerY = 0.5f,
                    sizePx = sizePx,
                    behavior = behavior,
                    random = Random(seed),
                ).also { eye ->
                    scleraColor?.let { eye.scleraColor = it }
                    irisColor?.let { eye.irisColor = it }
                    pupilColor?.let { eye.pupilColor = it }
                },
            ),
        )
    }

    Box(modifier = modifier) {
        EyeCanvas(
            state = state,
            modifier = Modifier.fillMaxSize(),
            canvasColor = canvasColor,
        )
    }
}
