package com.dangerfield.movingeyes.libraries.ui.system.color

import androidx.compose.animation.VectorConverter
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.animateValueAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.dangerfield.movingeyes.system.Dimension
import com.dangerfield.movingeyes.system.Radii
import com.dangerfield.movingeyes.system.VerticalSpacerD100
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.math.roundToInt

@Suppress("ClassNaming")
@Deprecated("AVOID USING COLOR RESOURCES DIRECTLY. Instead opt for AppTheme.colors.xyz for semantic colors")
@Stable
sealed class ColorResource(val color: Color, val designSystemName: String) {
    object Unspecified : ColorResource(Color.Unspecified, "unspecified")

    /*
     * The Safelight ramp. Darkroom equipment, not a novelty app: everything is
     * warm graphite on true black, and a single amber accent — the colour of a
     * photographic safelight — carries every interactive, selected and live
     * state. Amber reads as Halloween without a single pumpkin, and it's the
     * one hue that stays legible against the eye colours on the canvas.
     *
     * Canvas is the only true black. Chrome sits on Surface1 or above, so the
     * canvas edge always reads as an actual edge.
     */
    object Canvas : ColorResource(Color(0xFF000000), "canvas")
    object Surface1 : ColorResource(Color(0xFF0C0A0A), "surface-1")
    object Surface2 : ColorResource(Color(0xFF151212), "surface-2")
    object Surface3 : ColorResource(Color(0xFF1E1B1B), "surface-3")
    object Line : ColorResource(Color(0xFF2E2A29), "line")
    object LineStrong : ColorResource(Color(0xFF403B39), "line-strong")

    object TextHi : ColorResource(Color(0xFFEDE9E2), "text-hi")
    object TextMid : ColorResource(Color(0xFF9B978F), "text-mid")
    object TextLow : ColorResource(Color(0xFF6B6862), "text-low")

    object Accent : ColorResource(Color(0xFFF2A24B), "accent")
    object AccentBright : ColorResource(Color(0xFFFFBD73), "accent-bright")
    /** Amber at reading weight on a plate — borders, inactive accent strokes. */
    object AccentDeep : ColorResource(Color(0xFF6E5233), "accent-deep")
    /** Barely-there amber tint for the fill behind a selected control. */
    object AccentWash : ColorResource(Color(0xFF160D03), "accent-wash")

    object Danger : ColorResource(Color(0xFFE06A4E), "danger")
    object DangerDeep : ColorResource(Color(0xFF2A100C), "danger-deep")

    // Utility colors
    object Black : ColorResource(Color(0xFF000000), "black")
    object Black_A70 : ColorResource(Color(0xFF000000).copy(alpha = 0.7f), "black-a-70")
    object Black_A30 : ColorResource(Color(0xFF000000).copy(alpha = 0.3f), "black-a-30")
    object Black_A10 : ColorResource(Color(0xFF000000).copy(alpha = 0.1f), "black-a-10")

    object White : ColorResource(Color(0xFFFFFFFF), "white")
    object White_A70 : ColorResource(Color(0xFFFFFFFF).copy(alpha = 0.7f), "white-a-70")
    object White_A30 : ColorResource(Color(0xFFFFFFFF).copy(alpha = 0.3f), "white-a-30")

    class FromColor(color: Color, name: String) : ColorResource(color, name)

    val onColor: ColorResource
        get() {
            return if (color.luminance() > 0.4) Black else White
        }

    fun withAlpha(alpha: Float) = FromColor(this.color.copy(alpha = alpha), this.designSystemName + "_a_${alpha}")
}

@Composable
fun animateColorResourceAsState(
    targetValue: ColorResource,
    animationSpec: AnimationSpec<ColorResource> = spring(),
    label: String = "ColorAnimation",
    finishedListener: ((ColorResource) -> Unit)? = null,
): State<ColorResource> {
    val converter: TwoWayConverter<ColorResource, AnimationVector4D> = remember(targetValue) {
        val colorConverter = (Color.VectorConverter)(targetValue.color.colorSpace)
        TwoWayConverter(convertToVector = { token: ColorResource ->
            colorConverter.convertToVector(token.color)
        }, convertFromVector = { vector ->
            ColorResource.FromColor(
                color = colorConverter.convertFromVector(vector),
                name = targetValue.designSystemName
            )
        })
    }

    return animateValueAsState(
        targetValue = targetValue,
        typeConverter = converter,
        animationSpec = animationSpec,
        label = label,
        finishedListener = finishedListener
    )
}

private val colors = listOf(
    ColorResource.Canvas,
    ColorResource.Surface1,
    ColorResource.Surface2,
    ColorResource.Surface3,
    ColorResource.Line,
    ColorResource.LineStrong,
    ColorResource.TextHi,
    ColorResource.TextMid,
    ColorResource.TextLow,
    ColorResource.Accent,
    ColorResource.AccentBright,
    ColorResource.AccentDeep,
    ColorResource.AccentWash,
    ColorResource.Danger,
    ColorResource.DangerDeep,
    ColorResource.Black,
    ColorResource.White
)

@Preview(widthDp = 2000, heightDp = 10000, showBackground = false)
@Composable
private fun PreviewColorSwatch() {
    LazyVerticalGrid(
        columns = GridCells.Fixed(10)
    ) {
        items(colors) { colorResource ->
            ColorCard(
                colorResource,
                title = colorResource.designSystemName,
                description = colorResource.toHexString()
            )
        }
    }
}


@Composable
internal fun ColorCard(
    colorResource: ColorResource,
    title: String,
    description: String
) {
    Box(
        modifier = Modifier.Companion.padding(Dimension.D100)
            .background(colorResource.color, shape = Radii.Card.shape)
            .height(150.dp)
            .width(120.dp)
            .clip(Radii.Card.shape),
        contentAlignment = Alignment.BottomCenter
    ) {


        Column {
            if (colorResource.color.luminance() > 0.5f) {
                HorizontalDivider(
                    color = Color.DarkGray,
                )
            }

            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(Dimension.D500)
            ) {

                Text(
                    text = title,
                    color = Color.Black
                )

                VerticalSpacerD100()

                Text(
                    text = description,
                    color = Color.Black
                )
            }
        }

    }
}

/**
 * Extension function to convert a Color object to a hexadecimal string representation.
 * Includes the alpha value by default but can be omitted.
 *
 * @param includeAlpha whether to include the alpha value in the hex string.
 * @return A hex string representation of the color (e.g., "#FFFFFFFF" or "#FFFFFF" if alpha is omitted).
 */
fun ColorResource.toHexString(includeAlpha: Boolean = true): String {
    val color = this.color

    // Handle unspecified color explicitly
    if (color == Color.Unspecified) return "unspecified"

    fun componentToHex(component: Float): String {
        val intVal = (component * 255f).coerceIn(0f, 255f).roundToInt()
        return intVal.toString(16).uppercase().padStart(2, '0')
    }

    val alpha = if (includeAlpha) componentToHex(color.alpha) else ""
    val red = componentToHex(color.red)
    val green = componentToHex(color.green)
    val blue = componentToHex(color.blue)
    return "#${alpha}${red}${green}${blue}"
}