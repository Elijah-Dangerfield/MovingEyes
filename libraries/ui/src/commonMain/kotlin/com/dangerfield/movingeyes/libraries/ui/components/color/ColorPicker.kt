@file:Suppress("MagicNumber")

package com.dangerfield.movingeyes.libraries.ui.components.color

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.dangerfield.movingeyes.system.Dimension
import androidx.compose.foundation.shape.RoundedCornerShape

/**
 * Saturation and value on a panel, hue on a slider below it.
 *
 * The layout everyone already knows, which matters more here than novelty: this
 * gets used once, at night, by someone who wants an orange eye and does not
 * want to learn a colour model to get one.
 *
 * The picker works in [Hsv] and only converts out. Round-tripping through
 * [Color] on every drag loses the hue of a black or fully desaturated colour,
 * so dragging to the bottom of the panel would snap the hue slider to red.
 */
@Composable
fun ColorPicker(
    hsv: Hsv,
    onHsvChange: (Hsv) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimension.D400),
    ) {
        SaturationValuePanel(
            hsv = hsv,
            onChange = { saturation, value ->
                onHsvChange(hsv.copy(saturation = saturation, value = value))
            },
        )
        HueSlider(
            hue = hsv.hue,
            onHueChange = { onHsvChange(hsv.copy(hue = it)) },
        )
    }
}

@Composable
private fun SaturationValuePanel(hsv: Hsv, onChange: (Float, Float) -> Unit) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(PanelHeight)
            .clip(RoundedCornerShape(PanelCorner))
            .pickable { position, size ->
                onChange(
                    (position.x / size.width).coerceIn(0f, 1f),
                    1f - (position.y / size.height).coerceIn(0f, 1f),
                )
            },
    ) {
        drawRect(
            Brush.horizontalGradient(listOf(Color.White, Hsv(hsv.hue, 1f, 1f).toColor())),
        )
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))

        drawThumb(
            center = Offset(
                x = hsv.saturation * size.width,
                y = (1f - hsv.value) * size.height,
            ),
            fill = hsv.toColor(),
        )
    }
}

@Composable
private fun HueSlider(hue: Float, onHueChange: (Float) -> Unit) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(SliderHeight)
            .clip(RoundedCornerShape(percent = 50))
            .pickable { position, size ->
                onHueChange((position.x / size.width).coerceIn(0f, 1f) * 360f)
            },
    ) {
        drawRect(
            Brush.horizontalGradient(
                (0..6).map { Hsv(it * 60f, 1f, 1f).toColor() },
            ),
        )
        drawThumb(
            center = Offset(x = hue / 360f * size.width, y = size.height / 2f),
            fill = Hsv(hue, 1f, 1f).toColor(),
        )
    }
}

/**
 * A tap places the thumb and a drag moves it, both from the first touch, so the
 * colour follows the finger immediately rather than after a slop threshold.
 */
private fun Modifier.pickable(onPick: (Offset, Size) -> Unit): Modifier =
    this
        .pointerInput(Unit) {
            detectTapGestures { onPick(it, size.toSize()) }
        }
        .pointerInput(Unit) {
            detectDragGestures(
                onDragStart = { onPick(it, size.toSize()) },
            ) { change, _ ->
                onPick(change.position, size.toSize())
                change.consume()
            }
        }

/** White ring over a black one, so the thumb stays visible on every colour
 *  including white and black. */
private fun DrawScope.drawThumb(center: Offset, fill: Color) {
    val radius = ThumbRadius.toPx()
    drawCircle(color = fill, radius = radius, center = center)
    drawCircle(
        color = Color.Black.copy(alpha = 0.5f),
        radius = radius,
        center = center,
        style = Stroke(width = ThumbStroke.toPx() * 2f),
    )
    drawCircle(
        color = Color.White,
        radius = radius,
        center = center,
        style = Stroke(width = ThumbStroke.toPx()),
    )
}

private val PanelHeight: Dp = 160.dp
private val PanelCorner: Dp = 12.dp
private val SliderHeight: Dp = 28.dp
private val ThumbRadius: Dp = 9.dp
private val ThumbStroke: Dp = 2.dp
