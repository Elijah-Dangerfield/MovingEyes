package com.dangerfield.movingeyes.libraries.render

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.dangerfield.movingeyes.libraries.eyes.EyeStyles
import com.dangerfield.movingeyes.libraries.eyes.SceneDirector
import com.dangerfield.movingeyes.libraries.eyes.Mood
import com.dangerfield.movingeyes.libraries.scene.Scene
import com.dangerfield.movingeyes.libraries.scene.SceneEye
import kotlin.math.min
import kotlin.random.Random

/**
 * The boundary between a scene as stored and a scene as drawn. A [SceneEye] is
 * portable; a [RenderedEye] is pixels on this canvas with a running state
 * machine attached.
 */

/**
 * Seeded by index, not randomly: distinct per eye so a pair never blinks in
 * lockstep, but stable so reopening a scene gives back what you saved.
 */
fun Scene.toRenderedEyes(
    canvasWidthPx: Float,
    canvasHeightPx: Float,
    gaze: SceneDirector? = null,
): List<RenderedEye> {
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
            sceneDirector = gaze,
        )
    }
}

/**
 * [moods] comes in separately because a [RenderedEye] holds only the resolved
 * config and can't say which named mood produced it. Storing the config
 * instead would freeze today's numbers for Frantic into every saved scene.
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

/** Masked because `toArgb` returns a signed Int and an opaque colour has its
 *  top bit set, which would sign-extend. */
private fun Color.toArgbLong(): Long = toArgb().toLong() and 0xFFFFFFFFL
