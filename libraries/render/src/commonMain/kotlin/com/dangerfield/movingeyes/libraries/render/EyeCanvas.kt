@file:Suppress("MagicNumber")

package com.dangerfield.movingeyes.libraries.render

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate

/**
 * Every eye in the scene, in one Canvas, driven by one frame clock.
 *
 * ## Why it's built this way
 *
 * **One Canvas, not one composable per eye.** Twenty-four composables each
 * animating themselves would be twenty-four recompositions a frame; this is
 * one draw pass with no recomposition at all.
 *
 * **State lives outside composition.** [EyeSceneState] holds mutable fields
 * that the frame loop writes and the draw lambda reads. Nothing in the update
 * path is Compose state, so advancing a frame doesn't invalidate composition
 * or layout — it only invalidates the draw phase. On a five-year-old tablet
 * that is the difference between smooth and janky, and it is the single most
 * important thing in this file.
 *
 * The one Compose state read is [EyeSceneState.frameTick], and it exists
 * purely to tell Compose "the drawing changed" without telling it anything
 * about *what* changed.
 *
 * **30fps, not 60.** Eyes move slowly, nobody can tell, and it roughly halves
 * battery draw over a five-hour session — which is the actual product
 * requirement. `withFrameNanos` fires at the display's rate, so frames are
 * accumulated and dropped rather than requested.
 */
@Composable
fun EyeCanvas(
    state: EyeSceneState,
    modifier: Modifier = Modifier,
    canvasColor: Color = Color.Black,
) {
    LaunchedEffect(state) {
        var lastFrameNanos = 0L
        var accumulated = 0f

        while (true) {
            withFrameNanos { now ->
                if (lastFrameNanos != 0L) {
                    val delta = (now - lastFrameNanos) / NanosPerSecond
                    accumulated += delta

                    val target = state.frameIntervalSeconds
                    if (accumulated >= target) {
                        state.advance(accumulated)
                        accumulated = 0f
                    }
                }
                lastFrameNanos = now
            }
        }
    }

    Canvas(modifier = modifier) {
        // The only Compose state read in the whole render path. Touching it
        // inside the draw lambda subscribes the draw phase — and nothing else
        // — to the frame loop.
        @Suppress("UNUSED_EXPRESSION")
        state.frameTick

        drawRect(canvasColor)
        state.eyes.forEach { eye -> drawEye(eye, canvasColor) }
    }
}

private const val NanosPerSecond = 1_000_000_000f

/**
 * Draws one eye at its current animated state.
 *
 * The geometry is ported from `Eye.dc.html` so the shipped renderer and the
 * design prototype agree; the layer order is sclera → veins → iris → pupil →
 * glint → lid, and the lid is drawn in the **canvas colour** rather than as an
 * alpha mask, which is what makes a closed eye read as a hole in the void
 * against black rather than a grey smear.
 */
private fun DrawScope.drawEye(eye: RenderedEye, canvasColor: Color) {
    val frame = eye.runtime.frame
    val style = eye.style

    val width = eye.sizePx
    val height = width * style.aspectRatio
    val center = Offset(eye.centerX * size.width, eye.centerY * size.height)
    val brushes = eye.brushes()

    rotate(degrees = eye.rotationDegrees, pivot = center) {
        translate(left = center.x, top = center.y) {
            val irisRadius = width * style.irisRatio / 2f

            // Glow first, under everything, and deliberately outside the eye
            // clip below — a bloom is light escaping past the eye's edge, so
            // clipping it to the eye would delete the entire effect. This is
            // the most expensive thing here, which is why low-power mode turns
            // it off entirely.
            brushes.glow?.let { glow ->
                drawCircle(
                    brush = glow,
                    radius = irisRadius + eye.glowPx,
                    center = Offset.Zero,
                    alpha = (0.55f + frame.arousal * 0.45f),
                )
            }

            // Everything from here is clipped to the eye's own ellipse.
            //
            // Not cosmetic: several styles have an iris wider or taller than
            // the eye that holds it — Reptile is 0.70 iris against a 0.66
            // aspect ratio, Demon 0.82 against 0.72 — because that squeezed,
            // too-big-for-its-socket look is the whole point of those styles.
            // Without the clip the iris and its slit simply spill out over the
            // sclera and read as a rendering fault.
            val eyeBounds = Path().apply {
                addOval(
                    androidx.compose.ui.geometry.Rect(
                        offset = Offset(-width / 2f, -height / 2f),
                        size = androidx.compose.ui.geometry.Size(width, height),
                    ),
                )
            }

            clipPath(eyeBounds) {
                if (style.scleraOpacity > 0f) {
                    scale(scaleX = 1f, scaleY = style.aspectRatio, pivot = Offset.Zero) {
                        drawCircle(
                            brush = brushes.sclera,
                            radius = width / 2f,
                            center = Offset.Zero,
                            alpha = style.scleraOpacity,
                        )
                    }
                    if (style.hasVeins) drawVeins(width, height, eye.veinIntensity)
                }

                // The pupil travels, the sclera doesn't. Gaze is scaled by the
                // room between the iris edge and the eye edge, so a big iris
                // (Feline, Glow Orb) simply has less room to move — which is
                // correct, and keeps the iris from wandering off the eye.
                val travelX = (width / 2f - irisRadius).coerceAtLeast(0f)
                val travelY = (height / 2f - irisRadius).coerceAtLeast(0f)
                val gaze = Offset(frame.gaze.x * travelX, frame.gaze.y * travelY)

                translate(left = gaze.x, top = gaze.y) {
                    drawCircle(
                        brush = brushes.iris,
                        radius = irisRadius,
                        center = Offset.Zero,
                    )

                    drawPupil(style, irisRadius, frame.pupilScale, eye.pupilColor)

                    // Specular dot, up and to the left. The cheapest thing in
                    // the whole renderer and the one that makes an eye look wet.
                    val glintRadius = width * style.glintRatio * 0.3f
                    if (glintRadius > 0.5f) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.85f),
                            radius = glintRadius,
                            center = Offset(-irisRadius * 0.34f, -irisRadius * 0.42f),
                        )
                    }
                }
            }

            drawLid(frame.lidOpenness, width, height, canvasColor)
        }
    }
}

private fun DrawScope.drawPupil(
    style: com.dangerfield.movingeyes.libraries.eyes.EyeStyle,
    irisRadius: Float,
    pupilScale: Float,
    pupilColor: Color,
) {
    val scale = pupilScale.coerceIn(0.4f, 2.2f)

    // A slit runs nearly the full height of the iris and a bar nearly its full
    // width, so both are rectangles longer than the circle they sit in — their
    // ends would otherwise poke out into the sclera and read as a rendering
    // bug rather than an eye. Clipping to the iris is also just what an iris
    // does: a cat's slit ends where the iris ends.
    val needsClip = style.slitPupil || style.barPupil
    if (!needsClip) {
        drawCircle(
            color = pupilColor,
            radius = irisRadius * style.pupilRatio * scale,
            center = Offset.Zero,
        )
        return
    }

    val irisBounds = Path().apply {
        addOval(
            androidx.compose.ui.geometry.Rect(
                center = Offset.Zero,
                radius = irisRadius,
            ),
        )
    }

    clipPath(irisBounds) {
        if (style.slitPupil) {
            drawRoundedBar(
                halfWidth = irisRadius * 0.20f * scale,
                halfHeight = irisRadius * 0.96f,
                color = pupilColor,
            )
        } else {
            drawRoundedBar(
                halfWidth = irisRadius * 0.92f,
                halfHeight = irisRadius * 0.22f * scale,
                color = pupilColor,
            )
        }
    }
}

/**
 * The lid closes from both edges toward the middle, drawn in the canvas
 * colour. Two rectangles rather than a scaled mask so a partly-closed lid on a
 * heavy-lidded mood looks like a lid and not a fade.
 */
private fun DrawScope.drawLid(
    openness: Float,
    width: Float,
    height: Float,
    canvasColor: Color,
) {
    if (openness >= 0.999f) return
    val closed = (1f - openness) * height / 2f
    val halfWidth = width / 2f + 1f

    drawRect(
        color = canvasColor,
        topLeft = Offset(-halfWidth, -height / 2f - 1f),
        size = androidx.compose.ui.geometry.Size(halfWidth * 2f, closed + 1f),
    )
    drawRect(
        color = canvasColor,
        topLeft = Offset(-halfWidth, height / 2f - closed),
        size = androidx.compose.ui.geometry.Size(halfWidth * 2f, closed + 1f),
    )
}

/**
 * Vessels creeping in from the corners.
 *
 * Curves, not straight lines: a straight red line across a sclera reads as a
 * scratch on the screen rather than a vein, which is the kind of detail that
 * makes someone think the app is broken instead of gruesome. They also start
 * at the corners and fade inward, because that's where they actually are.
 */
private fun DrawScope.drawVeins(width: Float, height: Float, intensity: Float) {
    if (intensity <= 0f) return
    val alpha = intensity.coerceIn(0f, 1f) * 0.45f
    val stroke = (width * 0.008f).coerceAtLeast(1f)
    val vein = Color(0xFFAA2828)

    listOf(-1f, 1f).forEach { side ->
        listOf(-0.10f, 0.14f, 0.30f).forEachIndexed { index, drift ->
            val path = Path().apply {
                moveTo(side * width * 0.49f, height * drift * 0.5f)
                quadraticTo(
                    side * width * 0.36f,
                    height * (drift - 0.06f),
                    side * width * 0.22f,
                    height * (drift * 0.4f - 0.02f),
                )
            }
            drawPath(
                path = path,
                color = vein.copy(alpha = alpha / (index + 1)),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke),
            )
        }
    }
}

/**
 * Mutable, non-Compose scene state. The frame loop writes it, the draw lambda
 * reads it. See [EyeCanvas] for why that split matters.
 */
@Stable
class EyeSceneState(
    eyes: List<RenderedEye> = emptyList(),
    /** 30fps by default; low-power mode drops this to 20. */
    frameIntervalSeconds: Float = 1f / 30f,
) {
    var eyes: List<RenderedEye> = eyes
    var frameIntervalSeconds: Float = frameIntervalSeconds

    /**
     * Bumped once per advanced frame. The draw lambda reads it so Compose
     * knows to redraw; nothing else in the app should ever look at it.
     */
    var frameTick by mutableIntStateOf(0)
        private set

    fun advance(deltaSeconds: Float) {
        eyes.forEach { it.runtime.advance(deltaSeconds) }
        frameTick += 1
    }

    /** A sound arrived. Every eye reacts, but not identically. */
    fun startleAll(direction: Float, intensity: Float = 1f) {
        eyes.forEach { it.runtime.startle(direction, intensity) }
    }
}
