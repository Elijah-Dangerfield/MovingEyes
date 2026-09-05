@file:Suppress("MagicNumber")

package com.dangerfield.movingeyes.libraries.render

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.dangerfield.movingeyes.libraries.eyes.BehaviorConfig
import com.dangerfield.movingeyes.libraries.eyes.EyeRuntime
import com.dangerfield.movingeyes.libraries.eyes.EyeStyle
import com.dangerfield.movingeyes.libraries.eyes.SceneDirector
import com.dangerfield.movingeyes.libraries.eyes.Moods
import kotlin.math.cos
import kotlin.math.sin
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
    sceneDirector: SceneDirector? = null,
) {
    val runtime = EyeRuntime(behavior, random, sceneDirector)

    /** Fixed at construction so an eye's iris texture doesn't reshuffle when it
     *  is recoloured or resized. Two eyes on one face have different irises;
     *  the same eye across two frames does not. */
    internal val fibres = irisFibres(random)

    /**
     * How far the pupil sits off the iris's centre, in iris radii.
     *
     * Real pupils are a little nasal and a little low, and vary between people
     * by more than you would guess. Drawn from the same seeded source as the
     * fibres so an eye keeps its own face across a resize or a recolour.
     */
    internal val pupilOffsetX = (random.nextFloat() - 0.5f) * PupilDrift
    internal val pupilOffsetY = (random.nextFloat() - 0.5f) * PupilDrift

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

    /** Mutable so restyling keeps position, size and running motion — changing
     *  style must not scatter an alignment. */
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

    /** A fraction of the eye's diameter, not a pixel count, so glow keeps
     *  matching its eye through a pinch. */
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
            lidShadow = lidShadowBrush(sizePx * style.aspectRatio),
            glint = glintBrush(sizePx * style.glintRatio * GlintRadiusRatio),
            fibreLight = irisColor.lighten(0.30f),
            fibreDark = irisColor.shade(-0.30f),
            limbal = irisColor.shade(-0.62f),
        )
    }
}

/**
 * The shape of an eye: an upper and a lower curve meeting at two corners.
 *
 * Both curves are cubics with their control points at the same height, which
 * puts the apex at exactly 3/4 of that height — hence [ApexToControl]. Pulling
 * the controls inward (a smaller reach) draws the corners to a point without
 * moving the apex, so [taper] changes how almond the eye is and nothing else.
 *
 * The lower curve is drawn tighter than the upper because a real lower lid is
 * flatter. A vertically symmetric almond reads as a leaf.
 */
internal class Aperture(width: Float, height: Float, taper: Float) {

    val halfWidth = width / 2f
    private val rise = height / 2f / ApexToControl
    private val upperReach = halfWidth * (1f - 0.42f * taper)
    private val lowerReach = halfWidth * (1f - 0.62f * taper)

    fun path(): Path = Path().apply {
        moveTo(-halfWidth, 0f)
        cubicTo(-upperReach, -rise, upperReach, -rise, halfWidth, 0f)
        cubicTo(lowerReach, rise, -lowerReach, rise, -halfWidth, 0f)
        close()
    }

    /**
     * Everything one lid covers once it has descended [travel].
     *
     * The lid edge is the aperture's own curve slid across the eye, so the
     * margin of a half-closed eye stays parallel to the shape it is closing —
     * which is what makes a blink look like a lid rather than a crop.
     */
    fun lid(travel: Float, isUpper: Boolean): Path {
        val direction = if (isUpper) 1f else -1f
        val reach = if (isUpper) upperReach else lowerReach
        val edge = travel * direction
        val backstop = (rise + travel) * -direction

        return Path().apply {
            moveTo(-halfWidth, edge)
            cubicTo(-reach, -rise * direction + edge, reach, -rise * direction + edge, halfWidth, edge)
            lineTo(halfWidth, backstop)
            lineTo(-halfWidth, backstop)
            close()
        }
    }
}

/** Where a cubic with level control points puts its apex, as a fraction of the
 *  control height. */
private const val ApexToControl = 0.75f

/** Cached per eye. See [RenderedEye.brushes]. */
internal class EyeBrushes(
    val sclera: Brush,
    val iris: Brush,
    val glow: Brush?,
    val lidShadow: Brush,
    val glint: Brush,
    val fibreLight: Color,
    val fibreDark: Color,
    val limbal: Color,
)

/**
 * A little shading toward the top of the eyeball.
 *
 * Deliberately faint. This started as an upper lid's cast shadow, which is
 * wrong for where these actually get used: taped behind a painting with a hole
 * cut in it, the cardboard *is* the lid, and the app has no idea where its edge
 * falls. Drawing a second lid inside the hole fights the real one.
 *
 * What survives is the eyeball's own curvature — a sphere is darker where it
 * turns away from you — which reads at any aperture and does not pretend to
 * know about a lid it cannot see.
 */
internal fun lidShadowBrush(height: Float): Brush = Brush.verticalGradient(
    0f to Color.Black.copy(alpha = 0.15f),
    0.55f to Color.Black.copy(alpha = 0.03f),
    1f to Color.Transparent,
    startY = -height / 2f,
    endY = height * 0.1f,
)

/**
 * The catchlight, as a soft-edged gradient rather than a hard dot.
 *
 * A crisp white circle reads as a sticker; the falloff is what reads as a
 * reflection on a wet curved surface.
 */
internal fun glintBrush(radius: Float): Brush = Brush.radialGradient(
    0f to Color.White.copy(alpha = 0.95f),
    0.62f to Color.White.copy(alpha = 0.80f),
    1f to Color.White.copy(alpha = 0f),
    center = Offset.Zero,
    radius = radius.coerceAtLeast(0.5f),
)

/**
 * The radial striations of an iris, as a flat array of
 * `[cos, sin, innerRadiusFraction, signedAlpha]`.
 *
 * A flat disc of colour is the last thing standing between this and something
 * that looks photographed. Real fibres run from the pupil to the limbus at
 * uneven lengths and alternate lighter and darker than the iris around them,
 * which is what the sign on the alpha encodes.
 *
 * Kept as a `FloatArray` rather than objects because it is walked once per eye
 * per frame, and a list of small classes there is a list of pointer chases.
 */
internal fun irisFibres(random: Random): FloatArray {
    val data = FloatArray(FibreCount * 4)
    var index = 0

    repeat(FibreCount) { spoke ->
        // Jittered off a regular spacing: evenly spaced fibres look machined,
        // fully random ones clump and leave bald patches.
        val angle = (spoke + random.nextFloat() * 0.7f - 0.35f) / FibreCount * TwoPi
        val magnitude = 0.10f + random.nextFloat() * 0.22f

        data[index++] = cos(angle)
        data[index++] = sin(angle)
        data[index++] = 0.30f + random.nextFloat() * 0.28f
        data[index++] = if (random.nextBoolean()) magnitude else -magnitude
    }
    return data
}

/** Of the style's declared glint width. Big enough to catch the eye, small
 *  enough that it doesn't flatten the fibres under it. */
internal const val GlintRadiusRatio = 0.30f

/** Peak pupil offset in iris radii. Big enough to break the symmetry, small
 *  enough that nobody reads it as a lazy eye. */
private const val PupilDrift = 0.09f

private const val FibreCount = 24
private const val TwoPi = 6.2831855f

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
