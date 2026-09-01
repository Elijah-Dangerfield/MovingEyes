@file:Suppress("MagicNumber")

package com.dangerfield.movingeyes.libraries.render

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.dangerfield.movingeyes.libraries.eyes.BehaviorConfig
import com.dangerfield.movingeyes.libraries.eyes.EyeRuntime
import com.dangerfield.movingeyes.libraries.eyes.EyeStyle
import com.dangerfield.movingeyes.libraries.eyes.Moods
import kotlin.random.Random

/**
 * One eye, ready to draw: where it is, how it looks, and the running state
 * machine that decides where it's looking this instant.
 *
 * Position is normalised 0..1 of the canvas so a scene survives a resolution
 * change, a device rotation, or being opened on a different tablet — which is
 * the whole reason a scene is portable at all.
 */
class RenderedEye(
    style: EyeStyle,
    /** 0..1 of canvas width. */
    var centerX: Float,
    /** 0..1 of canvas height. */
    var centerY: Float,
    sizePx: Float,
    var rotationDegrees: Float = 0f,
    scleraColor: Color = Color(style.defaultSclera),
    irisColor: Color = Color(style.defaultIris),
    var pupilColor: Color = Color(style.defaultPupil),
    var veinIntensity: Float = 0.5f,
    glowFraction: Float = style.defaultGlow / 100f,
    behavior: BehaviorConfig = Moods.FreeDefault,
    random: Random = Random.Default,
) {
    val runtime = EyeRuntime(behavior, random)

    /**
     * The gradient brushes, built once and reused until something they depend
     * on changes.
     *
     * This is not premature optimisation. A `Brush` is where Compose caches
     * its compiled shader, so allocating a fresh one inside the draw lambda
     * throws that cache away and recompiles the gradient **every frame, for
     * every eye** — at the 24-eye cap that's over two thousand shader builds a
     * second, for five hours, on a tablet running off a battery.
     *
     * Nothing here depends on gaze, blink or dilation, which is why it can be
     * cached at all: those move the pupil, they don't repaint the iris.
     */
    private var brushes: EyeBrushes? = null

    /**
     * Mutable so the Look panel can restyle a selection in place. Swapping the
     * style keeps the eye's position, size and running motion — turning two
     * Human eyes into two Demon eyes must not scatter an alignment the user
     * already got right.
     */
    var style: EyeStyle = style
        set(value) {
            if (field != value) { field = value; brushes = null }
        }

    var sizePx: Float = sizePx
        set(value) {
            if (field != value) { field = value; brushes = null }
        }

    var scleraColor: Color = scleraColor
        set(value) {
            if (field != value) { field = value; brushes = null }
        }

    var irisColor: Color = irisColor
        set(value) {
            if (field != value) { field = value; brushes = null }
        }

    /**
     * Bloom radius as a fraction of the eye's own diameter, not an absolute
     * pixel count.
     *
     * Absolute would mean a glow set on a small eye vanishes the moment you
     * pinch it larger, and swamps it when you pinch it smaller — the glow would
     * silently stop matching the eye it belongs to every time anyone resized
     * anything.
     */
    var glowFraction: Float = glowFraction
        set(value) {
            if (field != value) { field = value; brushes = null }
        }

    val glowPx: Float get() = glowFraction * sizePx

    internal fun brushes(): EyeBrushes = brushes ?: buildBrushes().also { brushes = it }

    private fun buildBrushes(): EyeBrushes {
        val irisRadius = sizePx * style.irisRatio / 2f
        return EyeBrushes(
            sclera = scleraBrush(scleraColor, sizePx / 2f),
            iris = irisBrush(irisColor, irisRadius, style.isMilky),
            glow = if (glowPx > 0f) radialFade(irisColor, irisRadius + glowPx) else null,
        )
    }
}

/** Cached per eye. See [RenderedEye.brushes]. */
internal class EyeBrushes(
    val sclera: Brush,
    val iris: Brush,
    val glow: Brush?,
)

/**
 * Sclera: a highlight up and to the left, falling off to a shaded rim. The
 * off-centre highlight is what gives a flat circle its curvature.
 */
internal fun scleraBrush(sclera: Color, radius: Float): Brush = Brush.radialGradient(
    0f to Color.White,
    0.46f to sclera,
    1f to sclera.shade(-0.28f),
    center = Offset(-radius * 0.32f, -radius * 0.44f),
    radius = radius * 1.6f,
)

/**
 * Iris: lit from the same direction as the sclera, with a dark limbal rim.
 * A milky style flattens the gradient and centres it, so the eye can't appear
 * to focus — which is exactly why Ghoul is unsettling.
 */
internal fun irisBrush(iris: Color, radius: Float, milky: Boolean): Brush =
    if (milky) {
        Brush.radialGradient(
            0f to iris.lighten(0.25f),
            0.7f to iris,
            1f to iris.shade(-0.2f),
            center = Offset.Zero,
            radius = radius * 1.3f,
        )
    } else {
        Brush.radialGradient(
            0f to iris.lighten(0.34f),
            0.52f to iris,
            1f to iris.shade(-0.45f),
            center = Offset(-radius * 0.2f, -radius * 0.32f),
            radius = radius * 1.4f,
        )
    }

/** Bloom: the iris colour fading to nothing, drawn under the eye. */
internal fun radialFade(color: Color, radius: Float): Brush = Brush.radialGradient(
    0f to color.copy(alpha = 0.55f),
    0.45f to color.copy(alpha = 0.22f),
    1f to color.copy(alpha = 0f),
    center = Offset.Zero,
    radius = radius,
)

/** A slit or bar pupil. Rounded ends, because a hard rectangle reads as a
 *  rendering artifact rather than an eye. */
internal fun DrawScope.drawRoundedBar(halfWidth: Float, halfHeight: Float, color: Color) {
    drawRoundRect(
        color = color,
        topLeft = Offset(-halfWidth, -halfHeight),
        size = Size(halfWidth * 2f, halfHeight * 2f),
        cornerRadius = CornerRadius(halfWidth, halfWidth),
    )
}

/** Matches the reference implementation's `shade()`: a flat move toward or
 *  away from white, rather than a perceptual blend. Cheap, and what the
 *  design prototype's colours were picked against. */
internal fun Color.shade(amount: Float): Color = Color(
    red = (red + amount).coerceIn(0f, 1f),
    green = (green + amount).coerceIn(0f, 1f),
    blue = (blue + amount).coerceIn(0f, 1f),
    alpha = alpha,
)

internal fun Color.lighten(amount: Float): Color = shade(amount)
