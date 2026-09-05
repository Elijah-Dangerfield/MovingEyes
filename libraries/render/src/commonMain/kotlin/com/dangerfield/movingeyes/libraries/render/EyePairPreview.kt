package com.dangerfield.movingeyes.libraries.render

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
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
import com.dangerfield.movingeyes.libraries.eyes.SceneDirector
import kotlin.random.Random

/**
 * Two eyes that belong to the same face.
 *
 * Not two [EyePreview]s side by side: each of those builds its own scene and
 * its own director, so the pair looks in different directions and blinks at
 * different moments. At the size a marketing strip uses, that disagreement is
 * the only thing anyone notices — it reads as a bug rather than as eyes.
 *
 * One canvas, one director, two eyes. Which is also what the real editor does,
 * and the reason this is worth having as its own primitive rather than a
 * paywall-shaped workaround.
 */
@Composable
fun EyePairPreview(
    style: EyeStyle,
    modifier: Modifier = Modifier,
    eyeSize: Dp = 56.dp,
    separation: Dp = 24.dp,
    behavior: BehaviorConfig = Moods.FreeDefault,
    canvasColor: Color = Color.Black,
) {
    val density = LocalDensity.current
    val width = eyeSize * 2 + separation
    val widthPx = with(density) { width.toPx() }
    val eyePx = with(density) { eyeSize.toPx() } * EyeFill

    val state = remember(style, behavior, widthPx, eyePx) {
        val director = SceneDirector(behavior)
        EyeSceneState(
            eyes = listOf(0, 1).map { index ->
                RenderedEye(
                    style = style,
                    // Split the box in half and centre an eye in each, so the
                    // gap is the separation asked for whatever the eye's aspect.
                    centerX = if (index == 0) 0.25f else 0.75f,
                    centerY = 0.5f,
                    sizePx = eyePx,
                    behavior = behavior,
                    random = Random(index),
                    sceneDirector = director,
                )
            },
            gaze = director,
        )
    }

    Box(modifier = modifier.width(width).height(eyeSize)) {
        EyeCanvas(
            state = state,
            modifier = Modifier.fillMaxSize(),
            canvasColor = canvasColor,
        )
    }
}

/** Headroom inside the box for glow, which extends past the iris. */
private const val EyeFill = 0.72f
