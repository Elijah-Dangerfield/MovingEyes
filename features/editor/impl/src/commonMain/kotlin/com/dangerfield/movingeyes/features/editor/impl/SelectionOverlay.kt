@file:Suppress("MagicNumber")

package com.dangerfield.movingeyes.features.editor.impl

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
) {
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

        guides.forEach { drawGuide(it, accent) }

        if (selection.isEmpty()) return@Canvas
        val bounds = selectionBounds(eyes, selection, size.width, size.height) ?: return@Canvas
        drawSelectionBox(bounds, accent)
    }
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

private fun DrawScope.drawGuide(guide: SnapGuide, accent: Color) {
    val stroke = 1.dp.toPx()
    // Dashed, so a guide can never be mistaken for something in the scene.
    val dash = PathEffect.dashPathEffect(floatArrayOf(12f, 10f))

    when (guide) {
        is SnapGuide.Vertical -> drawLine(
            color = accent,
            start = Offset(guide.x, 0f),
            end = Offset(guide.x, size.height),
            strokeWidth = stroke,
            pathEffect = dash,
        )

        is SnapGuide.Horizontal -> drawLine(
            color = accent,
            start = Offset(0f, guide.y),
            end = Offset(size.width, guide.y),
            strokeWidth = stroke,
            pathEffect = dash,
        )

        // Both bars at once. "These two gaps are the same" is not something
        // one line can say, and the pair you matched is as much the point as
        // the pair you're making.
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
