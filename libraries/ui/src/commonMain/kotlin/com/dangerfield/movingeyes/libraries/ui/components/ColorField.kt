package com.dangerfield.movingeyes.libraries.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.dangerfield.movingeyes.libraries.ui.PreviewContent
import com.dangerfield.movingeyes.libraries.ui.components.text.OutlinedTextField
import com.dangerfield.movingeyes.libraries.ui.components.text.Text
import com.dangerfield.movingeyes.system.AppTheme
import com.dangerfield.movingeyes.system.Dimension
import org.jetbrains.compose.ui.tooling.preview.Preview

private val SwatchCornerRadius = 10.dp
private val SwatchSize = 40.dp

/**
 * Sclera, iris, pupil, canvas. A swatch, a hex field, and recent colours.
 *
 * **Hex entry is not optional.** A hue wheel alone is unusable with low vision,
 * and it's also the wrong tool for the job here: people match an eye to a
 * colour they already have — a paint chip, a costume, a photo of a cat — and a
 * hex code is how they say it. The wheel is for browsing, the field is for
 * knowing.
 *
 * There is deliberately no eyedropper. It would mean a camera or a screen
 * capture, and the app asks for exactly one permission.
 *
 * [onColorChange] fires only on a **valid** six-digit hex, so a half-typed
 * value never repaints the canvas mid-keystroke.
 */
@Composable
fun ColorField(
    label: String,
    color: Color,
    onColorChange: (Color) -> Unit,
    modifier: Modifier = Modifier,
    recents: List<Color> = emptyList(),
) {
    var hexInput by remember(color) { mutableStateOf(color.toHexDigits()) }
    val isValid = hexInput.parseHexOrNull() != null

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimension.D400),
    ) {
        Text(
            text = label,
            typography = AppTheme.typography.Caption.C300,
            color = AppTheme.colors.textTertiary,
            allCaps = true,
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(Dimension.D500),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Swatch(color = color)

            OutlinedTextField(
                value = hexInput,
                onValueChange = { raw ->
                    hexInput = raw.filter { it.isHexDigit() }.take(HexDigits).uppercase()
                    hexInput.parseHexOrNull()?.let(onColorChange)
                },
                modifier = Modifier.weight(1f),
                isError = !isValid,
                singleLine = true,
                typographyToken = AppTheme.typography.Readout.R500,
                leadingIcon = {
                    Text(
                        text = "#",
                        typography = AppTheme.typography.Readout.R500,
                        color = AppTheme.colors.textTertiary,
                    )
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                ),
            )
        }

        if (recents.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(Dimension.D300)) {
                recents.forEach { recent ->
                    Swatch(
                        color = recent,
                        modifier = Modifier.clickable { onColorChange(recent) },
                    )
                }
            }
        }
    }
}

@Composable
private fun Swatch(color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(SwatchSize)
            .clip(RoundedCornerShape(SwatchCornerRadius))
            .background(color)
            .border(1.dp, AppTheme.colors.border.color, RoundedCornerShape(SwatchCornerRadius)),
    )
}

private const val HexDigits = 6

private fun Char.isHexDigit(): Boolean =
    this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'

/** `RRGGBB`, no leading `#` — the field renders the hash as a prefix, not as data. */
internal fun Color.toHexDigits(): String {
    fun channel(value: Float): String =
        (value * 255).toInt().coerceIn(0, 255).toString(16).uppercase().padStart(2, '0')
    return channel(red) + channel(green) + channel(blue)
}

/** Null until all six digits are present, so a partial entry can't repaint the canvas. */
internal fun String.parseHexOrNull(): Color? {
    if (length != HexDigits || !all { it.isHexDigit() }) return null
    val value = toLong(radix = 16)
    return Color(
        red = ((value shr 16) and 0xFF).toInt() / 255f,
        green = ((value shr 8) and 0xFF).toInt() / 255f,
        blue = (value and 0xFF).toInt() / 255f,
    )
}

@Preview
@Composable
private fun PreviewColorField() {
    PreviewContent(modifier = Modifier.padding(Dimension.D800)) {
        Column(verticalArrangement = Arrangement.spacedBy(Dimension.D700)) {
            ColorField(
                label = "Sclera",
                color = Color(0xFFF0E9D8),
                onColorChange = {},
                recents = listOf(Color(0xFFF1EBE0), Color(0xFFCFC9BE), Color(0xFF0E0C0E)),
            )
            ColorField(label = "Iris", color = Color(0xFFC8D24A), onColorChange = {})
            ColorField(label = "Pupil", color = Color(0xFF000000), onColorChange = {})
        }
    }
}
