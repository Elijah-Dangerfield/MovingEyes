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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import com.dangerfield.movingeyes.libraries.eyes.SceneDirector
import com.dangerfield.movingeyes.libraries.eyes.Moods
import kotlin.math.abs

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

/** Below one pixel there is nothing to draw and several brushes are undefined. */
private const val MinimumDrawableWidthPx = 1f

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

    // A gradient of radius zero throws rather than drawing nothing, so an eye
    // with no size has to be skipped before any brush is built. That happens on
    // the first frame of any preview, which lays out after it first draws.
    if (width < MinimumDrawableWidthPx) return
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
            val aperture = Aperture(width, height, style.cornerTaper)
            val eyeBounds = aperture.path()

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

                    // Skipped on small eyes rather than scaled down: below this
                    // the fibres land inside a pixel or two and turn into noise
                    // that costs 24 draw calls to render. A scenes-drawer
                    // miniature and a distant eye both want the flat disc.
                    if (irisRadius >= FibreVisibleRadiusPx) {
                        drawIrisFibres(eye.fibres, irisRadius, brushes)
                    }

                    // The limbus. A real iris ends in a dark ring, and its
                    // absence is most of why a drawn eye looks drawn.
                    drawCircle(
                        color = brushes.limbal.copy(alpha = 0.5f),
                        radius = irisRadius * 0.94f,
                        center = Offset.Zero,
                        style = Stroke(width = irisRadius * 0.12f),
                    )

                    // Nudged off the iris's centre. A real pupil sits slightly
                    // toward the nose and no two are identical, so a pair with
                    // both dead-centre is the last symmetry that reads as
                    // machined. Fixed per eye, not animated — it is anatomy.
                    translate(left = eye.pupilOffsetX * irisRadius, top = eye.pupilOffsetY * irisRadius) {
                        drawPupil(style, irisRadius, frame.pupilScale, eye.pupilColor)
                    }
                    drawGlints(brushes.glint, irisRadius, width * style.glintRatio * GlintRadiusRatio)
                }

                // Last, and over the iris as well as the sclera: a lid shadow
                // that stopped at the iris would be a lid behind the eyeball.
                drawRect(
                    brush = brushes.lidShadow,
                    topLeft = Offset(-width / 2f, -height / 2f),
                    size = Size(width, height),
                )

                // The wet inner corner. Small, warm, and asymmetric — it is the
                // only thing on the eye that tells you which way it faces.
                if (style.cornerTaper > 0.3f) {
                    drawCircle(
                        color = brushes.caruncle,
                        radius = width * CaruncleRatio,
                        center = Offset(-width / 2f + width * CaruncleRatio * 0.9f, 0f),
                    )
                }

                // Inside the clip so it hugs the aperture exactly, and after
                // everything else so the iris cannot sit on top of the lid.
                drawPath(
                    path = aperture.upperMargin(
                        (1f - frame.lidOpenness) * height * UpperLidShare,
                    ),
                    color = brushes.lashLine,
                    style = Stroke(
                        width = (height * LashRatio).coerceAtLeast(1f),
                        cap = StrokeCap.Round,
                    ),
                )
            }

            drawLids(frame.lidOpenness, aperture, height, canvasColor)
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
 * Lids that sweep along the eye's own curve rather than cutting straight across.
 *
 * The upper lid does [UpperLidShare] of a blink, as a real one does, so a
 * half-closed eye sits low the way a tired eye does instead of pinching shut
 * from both sides at once.
 *
 * Painted in the canvas colour rather than masked to alpha, so a closed eye
 * reads as a hole in the void instead of a grey smear.
 */
private fun DrawScope.drawLids(
    openness: Float,
    aperture: Aperture,
    height: Float,
    canvasColor: Color,
) {
    if (openness >= 0.999f) return
    val closed = (1f - openness) * height

    drawPath(aperture.lid(closed * UpperLidShare, isUpper = true), canvasColor)
    drawPath(aperture.lid(closed * (1f - UpperLidShare), isUpper = false), canvasColor)
}

/** How much of a blink the upper lid does. Real blinks are almost all upper. */
private const val UpperLidShare = 0.82f

/**
 * The striations running from pupil to limbus. See [irisFibres] for the layout
 * of the array; a negative alpha means the fibre is darker than the iris it
 * sits on rather than lighter.
 */
private fun DrawScope.drawIrisFibres(fibres: FloatArray, radius: Float, brushes: EyeBrushes) {
    val stroke = (radius * 0.05f).coerceAtLeast(0.8f)
    var index = 0

    while (index < fibres.size) {
        val cos = fibres[index]
        val sin = fibres[index + 1]
        val inner = fibres[index + 2]
        val signedAlpha = fibres[index + 3]
        index += 4

        drawLine(
            color = (if (signedAlpha < 0f) brushes.fibreDark else brushes.fibreLight)
                .copy(alpha = abs(signedAlpha)),
            start = Offset(cos * radius * inner, sin * radius * inner),
            end = Offset(cos * radius * 0.95f, sin * radius * 0.95f),
            strokeWidth = stroke,
        )
    }
}

/**
 * The main catchlight up and to the left, and the weak bounce opposite it.
 *
 * One highlight says "shiny"; two say "a wet sphere in a room with a floor".
 * Both ride the iris, so they move with the gaze the way a reflection does.
 */
private fun DrawScope.drawGlints(glint: Brush, irisRadius: Float, radius: Float) {
    if (radius <= 0.5f) return

    translate(left = -irisRadius * 0.34f, top = -irisRadius * 0.42f) {
        drawCircle(brush = glint, radius = radius, center = Offset.Zero)
    }
    translate(left = irisRadius * 0.30f, top = irisRadius * 0.40f) {
        drawCircle(brush = glint, radius = radius * 0.55f, center = Offset.Zero, alpha = 0.22f)
    }
}

/** Below this an iris is too small for fibres to be anything but noise. */
private const val FibreVisibleRadiusPx = 14f

/** Lash-line thickness, as a fraction of eye height. */
private const val LashRatio = 0.075f

/** Caruncle radius, as a fraction of eye width. */
private const val CaruncleRatio = 0.045f

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
    /**
     * Shared so every eye in the scene looks at the same thing. See
     * [SceneDirector] — a face looks at one thing, and eyes that each wander
     * separately read as a bag of unrelated eyeballs.
     */
    val gaze: SceneDirector = SceneDirector(eyes.firstOrNull()?.runtime?.behavior ?: Moods.FreeDefault),
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
        gaze.advance(deltaSeconds)
        eyes.forEach { it.runtime.advance(deltaSeconds) }
        frameTick += 1
    }

    /** A sound arrived. The scene looks toward it and every eye reacts. */
    fun startleAll(direction: Float, intensity: Float = 1f) {
        gaze.look(direction, intensity)
        eyes.forEach { it.runtime.startle(direction, intensity) }
    }
}
