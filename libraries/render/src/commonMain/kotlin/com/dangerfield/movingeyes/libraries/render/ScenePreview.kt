package com.dangerfield.movingeyes.libraries.render

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.IntSize
import com.dangerfield.movingeyes.libraries.eyes.Moods
import com.dangerfield.movingeyes.libraries.eyes.SceneDirector
import com.dangerfield.movingeyes.libraries.scene.Scene

/**
 * A whole scene, alive, shrunk into a chip.
 *
 * The composition *is* the scene — where the eyes sit relative to each other,
 * how many there are, how they differ in size. A single eye from the list tells
 * you none of that, and for the scenes built around an arrangement rather than
 * a colour (a nest, a row of windows, a low pair of embers) it is actively
 * misleading: you get one cropped eyeball and no way to tell two presets apart.
 *
 * Kept at the canvas's aspect ratio rather than square, because everything in a
 * [Scene] is normalised to the canvas. Squash the box and the composition
 * squashes with it, which is the one thing a preview must not do.
 */
@Composable
fun ScenePreview(
    scene: Scene,
    modifier: Modifier = Modifier,
    height: Dp = 56.dp,
    aspectRatio: Float = PortraitAspect,
    shape: Shape = RoundedCornerShape(6.dp),
) {
    var measured by remember { mutableStateOf(IntSize.Zero) }

    // The director is built first and handed to both, because an eye only
    // follows one it was constructed with. Without this the miniature's eyes
    // each ran their own gaze and blink — a pair of watchers looking at
    // different things, in a chip small enough that the disagreement is all
    // you can see.
    val state = remember(scene, measured) {
        val director = SceneDirector(
            behavior = scene.eyes.firstOrNull()?.behavior() ?: Moods.FreeDefault,
            blinksTogether = scene.blinkTogether,
        )
        EyeSceneState(
            eyes = scene.toRenderedEyes(
                canvasWidthPx = measured.width.toFloat(),
                canvasHeightPx = measured.height.toFloat(),
                gaze = director,
            ),
            gaze = director,
        )
    }

    Box(
        modifier = modifier
            .height(height)
            .aspectRatio(aspectRatio)
            .clip(shape)
            .onSizeChanged { measured = it },
    ) {
        if (measured == IntSize.Zero) return@Box

        EyeCanvas(
            state = state,
            modifier = Modifier.fillMaxSize(),
            canvasColor = Color(scene.canvasColor),
        )
    }
}

/** A tablet stood on its end: the orientation these scenes are built in. */
private const val PortraitAspect = 0.75f
