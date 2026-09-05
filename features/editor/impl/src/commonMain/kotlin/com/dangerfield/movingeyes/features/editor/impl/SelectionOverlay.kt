@file:Suppress("MagicNumber")

package com.dangerfield.movingeyes.features.editor.impl

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.drawText
import com.dangerfield.movingeyes.system.AppTheme
import com.dangerfield.movingeyes.libraries.render.RenderedEye
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The selection box and snap guides, drawn in their own Canvas **above** the
 * eyes.
 *
 * A separate Canvas on purpose: guides appear and vanish on touch, the eyes
 * redraw thirty times a second, and putting both in one draw pass would mean
 * every guide change re-issues every eye. Two layers keeps each one's
 * invalidation to itself.
 *
 * The whole overlay disappears in display mode. Chrome dissolves; the canvas
 * underneath never moves by a pixel.
 */
@Composable
fun SelectionOverlay(
    eyes: List<RenderedEye>,
    selection: List<Int>,
    guides: List<SnapGuide>,
    accent: Color,
    modifier: Modifier = Modifier,
    revision: Int = 0,
    /** Draws the device edge while the canvas is scaled down, so the scene has
     *  a boundary to read against instead of floating in black. */
    showCanvasEdge: Boolean = false,
    /** Faint centre crosshairs, shown while something is being dragged so the
     *  middle of the canvas is visible *before* you snap to it rather than only
     *  at the moment you arrive. */
    showCenterLines: Boolean = false,
    /**
     * Point-to-point dimension lines between the eyes, with the figure that
     * matters written on each one.
     *
     * The job this app is actually for is lining eyes up with holes someone is
     * about to cut, and for that a single number in a corner is the wrong
     * shape of answer — you need to know *which* gap is 62mm. Off by default:
     * the same lines are clutter once the alignment is done.
     */
    measurements: List<Measurement> = emptyList(),
) {
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = AppTheme.typography.Readout.R300.style.copy(color = accent)

    Canvas(modifier = modifier) {
        // Read so the overlay redraws while a drag is moving eyes that aren't
        // Compose state.
        @Suppress("UNUSED_EXPRESSION")
        revision

        if (showCanvasEdge) {
            drawRect(
                color = accent.copy(alpha = 0.25f),
                style = Stroke(width = 1.dp.toPx()),
            )
        }

        if (showCenterLines) drawCenterLines(accent)

        guides.forEach { drawGuide(it, accent) }

        measurements.forEach { drawMeasurement(it, accent, textMeasurer, labelStyle) }

        if (selection.isEmpty()) return@Canvas
        val bounds = selectionBounds(eyes, selection, size.width, size.height) ?: return@Canvas
        drawSelectionBox(bounds, accent)
    }
}

/**
 * A span worth showing, in canvas pixels, already labelled.
 *
 * [leader] is how far off the line between the two points the rule is drawn,
 * perpendicular to it. Non-zero pulls the rule clear of whatever it measures,
 * which is the only way two spans that share a bearing — a gap and the width of
 * the eye beside it — can be told apart at all.
 */
data class Measurement(
    val from: Offset,
    val to: Offset,
    val label: String,
    val leader: Float = 0f,
)

/**
 * A dimension line: a rule between two points with a tick at each end, drawn
 * the way a drawing does it rather than as a bare line, so it reads as a
 * measurement and not as a connection between two eyes.
 */
private fun DrawScope.drawMeasurement(
    measurement: Measurement,
    accent: Color,
    textMeasurer: TextMeasurer,
    labelStyle: TextStyle,
) {
    val along = measurement.to - measurement.from
    val length = along.getDistance()
    if (length < 1f) return

    val unit = along / length
    val normal = Offset(-unit.y, unit.x)
    val stroke = MeasurementStroke.toPx()
    val color = accent

    // Offset perpendicular, with a witness line back to each point it measures.
    // Without those the rule floats beside the thing it describes and the
    // number stops belonging to anything in particular.
    val shift = normal * measurement.leader
    val from = measurement.from + shift
    val to = measurement.to + shift
    val tick = normal * MeasurementTickHalf.toPx()

    if (measurement.leader != 0f) {
        val witness = color.copy(alpha = 0.35f)
        drawLine(witness, measurement.from, from + tick, strokeWidth = stroke)
        drawLine(witness, measurement.to, to + tick, strokeWidth = stroke)
    }

    drawLine(color = color, start = from, end = to, strokeWidth = stroke)
    drawLine(color = color, start = from - tick, end = from + tick, strokeWidth = stroke)
    drawLine(color = color, start = to - tick, end = to + tick, strokeWidth = stroke)

    // Above the rule and horizontal whatever angle the rule is at: a rotated
    // figure is harder to read than a level one, and the point of this is to be
    // read at arm's length.
    //
    // On its own plate, because the thing behind it is an arbitrary scene —
    // pale sclera, a glowing iris, black canvas — and text with no backing is
    // legible over exactly one of those.
    val label = textMeasurer.measure(measurement.label, labelStyle)
    val middle = (from + to) / 2f
    val padding = MeasurementLabelPadding.toPx()
    val topLeft = Offset(
        x = middle.x - label.size.width / 2f,
        y = middle.y - label.size.height - MeasurementLabelGap.toPx(),
    )

    drawRoundRect(
        color = Color.Black.copy(alpha = 0.82f),
        topLeft = Offset(topLeft.x - padding, topLeft.y - padding / 2f),
        size = Size(label.size.width + padding * 2f, label.size.height + padding),
        cornerRadius = CornerRadius(MeasurementLabelRadius.toPx()),
    )
    drawText(textLayoutResult = label, topLeft = topLeft)
}

/**
 * One box whether it holds one eye or twelve.
 *
 * Corners scale about the opposite corner and the rotate handle sits *above*
 * the box rather than on it, so a thumb never covers the thing it's turning —
 * which is the difference between rotating a pair accurately and guessing.
 */
private fun DrawScope.drawSelectionBox(bounds: Rect, accent: Color) {
    val stroke = 1.5.dp.toPx()

    drawRect(
        color = accent.copy(alpha = 0.7f),
        topLeft = bounds.topLeft,
        size = bounds.size,
        style = Stroke(width = stroke),
    )

    val handle = HandleSize.toPx()
    listOf(
        bounds.topLeft,
        Offset(bounds.right, bounds.top),
        Offset(bounds.left, bounds.bottom),
        Offset(bounds.right, bounds.bottom),
    ).forEach { corner ->
        drawRect(
            color = accent,
            topLeft = Offset(corner.x - handle / 2f, corner.y - handle / 2f),
            size = Size(handle, handle),
        )
    }

    // The rotate handle, on a stalk above the box.
    val stalkTop = rotateHandleCenter(bounds, RotateHandleGap.toPx())
    drawLine(
        color = accent.copy(alpha = 0.7f),
        start = Offset(bounds.center.x, bounds.top),
        end = stalkTop,
        strokeWidth = stroke,
    )
    drawCircle(
        color = accent,
        radius = RotateHandleRadius.toPx(),
        center = stalkTop,
        style = Stroke(width = stroke * 1.5f),
    )
}

private fun DrawScope.drawCenterLines(accent: Color) {
    val dash = PathEffect.dashPathEffect(floatArrayOf(6f, 14f))
    val faint = accent.copy(alpha = 0.3f)
    drawLine(
        color = faint,
        start = Offset(size.width / 2f, 0f),
        end = Offset(size.width / 2f, size.height),
        strokeWidth = 1.dp.toPx(),
        pathEffect = dash,
    )
    drawLine(
        color = faint,
        start = Offset(0f, size.height / 2f),
        end = Offset(size.width, size.height / 2f),
        strokeWidth = 1.dp.toPx(),
        pathEffect = dash,
    )
}

private fun DrawScope.drawGuide(guide: SnapGuide, accent: Color) {
    val stroke = 1.dp.toPx()
    // Dashed, so a guide can never be mistaken for something in the scene.
    val dash = PathEffect.dashPathEffect(floatArrayOf(12f, 10f))

    // A guide between two eyes spans them and a little past, rather than the
    // whole canvas. With fourteen eyes on screen a full-height rule says
    // "something here is aligned" and leaves you to work out what; a short one
    // points at the two things it is talking about. The canvas centre lines are
    // the exception and do run edge to edge, because the thing they align to
    // *is* the whole canvas.
    val overhang = GuideOverhang.toPx()

    when (guide) {
        is SnapGuide.Vertical -> drawLine(
            color = accent,
            start = Offset(guide.x, if (guide.from.isNaN()) 0f else guide.from - overhang),
            end = Offset(guide.x, if (guide.to.isNaN()) size.height else guide.to + overhang),
            strokeWidth = stroke,
            pathEffect = dash,
        )

        is SnapGuide.Horizontal -> drawLine(
            color = accent,
            start = Offset(if (guide.from.isNaN()) 0f else guide.from - overhang, guide.y),
            end = Offset(if (guide.to.isNaN()) size.width else guide.to + overhang, guide.y),
            strokeWidth = stroke,
            pathEffect = dash,
        )

        // Both bars at once. "These two gaps are the same" is not something
        // one line can say, and the pair you matched is as much the point as
        // the pair you're making.
        is SnapGuide.MatchedSize -> {
            drawMeasureBar(Offset(guide.fromA.x, guide.fromA.y), Offset(guide.toA.x, guide.toA.y), accent)
            drawMeasureBar(Offset(guide.fromB.x, guide.fromB.y), Offset(guide.toB.x, guide.toB.y), accent)
        }

        is SnapGuide.MatchedSpacing -> {
            drawMeasureBar(Offset(guide.fromA.x, guide.fromA.y), Offset(guide.toA.x, guide.toA.y), accent)
            drawMeasureBar(Offset(guide.fromB.x, guide.fromB.y), Offset(guide.toB.x, guide.toB.y), accent)
        }
    }
}

private fun DrawScope.drawMeasureBar(from: Offset, to: Offset, accent: Color) {
    val stroke = 1.5.dp.toPx()
    val cap = MeasureCapLength.toPx()

    drawLine(color = accent, start = from, end = to, strokeWidth = stroke)

    // End caps perpendicular to the bar, so it reads as a measurement rather
    // than a connection between two things.
    val direction = to - from
    val length = direction.getDistance().coerceAtLeast(0.001f)
    val perpendicular = Offset(-direction.y / length, direction.x / length) * (cap / 2f)

    listOf(from, to).forEach { end ->
        drawLine(
            color = accent,
            start = end - perpendicular,
            end = end + perpendicular,
            strokeWidth = stroke,
        )
    }
}

/** The box around a selection, in canvas pixels. */
fun selectionBounds(
    eyes: List<RenderedEye>,
    selection: List<Int>,
    canvasWidth: Float,
    canvasHeight: Float,
    paddingPx: Float = 12f,
): Rect? {
    val selected = selection.mapNotNull { eyes.getOrNull(it) }
    if (selected.isEmpty()) return null

    var left = Float.MAX_VALUE
    var top = Float.MAX_VALUE
    var right = -Float.MAX_VALUE
    var bottom = -Float.MAX_VALUE

    selected.forEach { eye ->
        val cx = eye.centerX * canvasWidth
        val cy = eye.centerY * canvasHeight
        val halfWidth = eye.sizePx / 2f
        val halfHeight = eye.sizePx * eye.style.aspectRatio / 2f

        // The rotated extent, not the upright one: a turned eye whose box still
        // hugged its unrotated shape would sit visibly off it.
        val radians = eye.rotationDegrees * PI.toFloat() / 180f
        val cosine = cos(radians)
        val sine = sin(radians)
        listOf(
            -halfWidth to -halfHeight,
            halfWidth to -halfHeight,
            -halfWidth to halfHeight,
            halfWidth to halfHeight,
        ).forEach { (dx, dy) ->
            val x = cx + dx * cosine - dy * sine
            val y = cy + dx * sine + dy * cosine
            left = minOf(left, x)
            top = minOf(top, y)
            right = maxOf(right, x)
            bottom = maxOf(bottom, y)
        }
    }

    return Rect(
        left = left - paddingPx,
        top = top - paddingPx,
        right = right + paddingPx,
        bottom = bottom + paddingPx,
    )
}

private val HandleSize: Dp = 14.dp
private val RotateHandleRadius: Dp = 13.dp
private val MeasureCapLength: Dp = 14.dp

private val MeasurementStroke: Dp = 2.dp
private val MeasurementTickHalf: Dp = 7.dp
private val MeasurementLabelGap: Dp = 6.dp
private val MeasurementLabelPadding: Dp = 5.dp
private val MeasurementLabelRadius: Dp = 6.dp

/** How far a guide runs past the things it aligns, so it reads as pointing at
 *  them rather than as touching them. */
private val GuideOverhang: Dp = 20.dp
