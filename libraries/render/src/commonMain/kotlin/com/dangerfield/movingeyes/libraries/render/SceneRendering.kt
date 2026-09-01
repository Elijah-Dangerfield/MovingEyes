package com.dangerfield.movingeyes.libraries.render

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.dangerfield.movingeyes.libraries.eyes.EyeStyles
import com.dangerfield.movingeyes.libraries.eyes.Mood
import com.dangerfield.movingeyes.libraries.scene.Scene
import com.dangerfield.movingeyes.libraries.scene.SceneEye
import kotlin.math.min
import kotlin.random.Random

/**
 * The boundary between a scene as *stored* and a scene as *drawn*.
 *
 * A [SceneEye] is portable and resolution-independent; a [RenderedEye] is
 * pixels on this canvas with a running state machine attached. Everything that
 * knows about that difference lives here, so nothing else has to.
 */

/**
 * Resolve a stored scene onto a canvas of this size.
 *
 * Each eye gets a distinct [Random], because two eyes sharing a source would
 * blink in lockstep and read as a screensaver — the one thing the behaviour
 * engine exists to avoid. The seed is the eye's index rather than a fresh
 * random, so reopening a scene gives back the composition you saved instead of
 * a subtly different one every time.
 */
fun Scene.toRenderedEyes(canvasWidthPx: Float, canvasHeightPx: Float): List<RenderedEye> {
    val shortEdge = min(canvasWidthPx, canvasHeightPx)
    return eyes.mapIndexed { index, eye ->
        RenderedEye(
            style = EyeStyles.byId(eye.styleId),
            centerX = eye.x,
            centerY = eye.y,
            sizePx = eye.sizeFraction * shortEdge,
            rotationDegrees = eye.rotationDegrees,
            scleraColor = Color(eye.scleraColor),
            irisColor = Color(eye.irisColor),
            pupilColor = Color(eye.pupilColor),
            veinIntensity = eye.veinIntensity,
            glowFraction = eye.glowFraction,
            behavior = eye.behavior(),
            random = Random(index),
        )
    }
}

/**
 * Capture the live eyes back into storable form.
 *
 * [moods] carries each eye's mood alongside it, because a [RenderedEye] holds
 * only the resolved [com.dangerfield.movingeyes.libraries.eyes.BehaviorConfig]
 * and can't say which named mood it came from — and a scene that stored the
 * config instead would lose the fact that the user picked "Frantic" and would
 * stop tracking any later change to what Frantic means.
 */
fun List<RenderedEye>.toSceneEyes(
    canvasWidthPx: Float,
    canvasHeightPx: Float,
    moods: List<Mood>,
): List<SceneEye> {
    val shortEdge = min(canvasWidthPx, canvasHeightPx)
    return mapIndexed { index, eye ->
        val mood = moods.getOrElse(index) { Mood.IdleScan }
        SceneEye(
            styleId = eye.style.id,
            x = eye.centerX,
            y = eye.centerY,
            sizeFraction = if (shortEdge > 0f) eye.sizePx / shortEdge else 0f,
            rotationDegrees = eye.rotationDegrees,
            scleraColor = eye.scleraColor.toArgbLong(),
            irisColor = eye.irisColor.toArgbLong(),
            pupilColor = eye.pupilColor.toArgbLong(),
            glowFraction = eye.glowFraction,
            veinIntensity = eye.veinIntensity,
            mood = mood,
            customBehavior = if (mood == Mood.Custom) eye.runtime.behavior else null,
        )
    }
}

/**
 * Back to the plain 0xAARRGGBB a scene stores. Masked because `toArgb` returns a
 * signed Int, and a colour with alpha set has its top bit on — widening that to
 * Long without the mask sign-extends into a value no colour parser will accept.
 */
private fun Color.toArgbLong(): Long = toArgb().toLong() and 0xFFFFFFFFL
