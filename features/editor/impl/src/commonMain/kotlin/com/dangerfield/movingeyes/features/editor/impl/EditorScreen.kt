package com.dangerfield.movingeyes.features.editor.impl

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import com.dangerfield.movingeyes.libraries.eyes.EyeStyles
import com.dangerfield.movingeyes.libraries.eyes.Moods
import com.dangerfield.movingeyes.libraries.render.EyeCanvas
import com.dangerfield.movingeyes.libraries.render.EyeSceneState
import com.dangerfield.movingeyes.libraries.render.RenderedEye
import kotlin.math.min
import kotlin.random.Random

/**
 * The canvas is the screen at 1:1 — no zoom, no pan, no insets. Black bleeds
 * to every edge, including under the notch, so on OLED the lit pixels are the
 * eyes and nothing else.
 *
 * The app opens straight onto this with two eyes already blinking. That's
 * deliberate and it replaced onboarding entirely: nobody reads three cards
 * before they've seen the thing work, and the product explains itself in about
 * a second if it's already moving when you arrive.
 *
 * Editing, selection and the panels land in the next phase. What's here now is
 * the scene, alive.
 */
@Composable
fun EditorScreen(modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current

        // Sized off the canvas rather than a fixed dp, because "two eyes" has
        // to look like a pair on a 5" phone and on a 13" tablet, portrait and
        // landscape. A fixed size that reads well on a tablet overlaps itself
        // on a phone.
        val state = remember(maxWidth, maxHeight) {
            val shortEdgePx = with(density) { min(maxWidth.toPx(), maxHeight.toPx()) }
            val eyeWidth = shortEdgePx * StartingEyeWidthFraction
            val separation = eyeWidth * StartingSeparationInEyeWidths
            val canvasWidthPx = with(density) { maxWidth.toPx() }
            val offset = (separation / 2f) / canvasWidthPx

            EyeSceneState(
                eyes = listOf(
                    eye(x = 0.5f - offset, sizePx = eyeWidth, seed = 1),
                    eye(x = 0.5f + offset, sizePx = eyeWidth, seed = 2),
                ),
            )
        }

        EyeCanvas(
            state = state,
            modifier = Modifier.fillMaxSize(),
            canvasColor = Color.Black,
        )
    }
}

/**
 * A pair of Human Basic eyes on the free motion default. Deliberately the
 * plainest thing the app can show: the first frame should look like a real
 * pair of eyes, not a demo of the spookiest style available.
 */
private fun eye(x: Float, sizePx: Float, seed: Int) = RenderedEye(
    style = EyeStyles.HumanBasic,
    centerX = x,
    centerY = 0.5f,
    sizePx = sizePx,
    behavior = Moods.FreeDefault,
    // A distinct seed per eye, so the pair never blinks in lockstep. Same rule
    // everywhere eyes are drawn, which is why it's a constructor argument
    // rather than a default.
    random = Random(seed),
)

/** Eye width as a fraction of the canvas's short edge. */
private const val StartingEyeWidthFraction = 0.30f

/**
 * Distance between pupils, in eye widths. Anatomically a face is closer to
 * 2.6, but that only reads right when the eyes are small relative to the
 * screen; at a size you can actually see across a room, a tighter pair looks
 * like a face and an anatomical one looks like two separate things.
 */
private const val StartingSeparationInEyeWidths = 1.35f
