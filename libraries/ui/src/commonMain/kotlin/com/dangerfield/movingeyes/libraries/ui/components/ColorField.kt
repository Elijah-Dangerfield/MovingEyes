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
import com.dangerfield.movingeyes.libraries.ui.components.color.ColorPicker
import com.dangerfield.movingeyes.libraries.ui.components.color.Hsv
import com.dangerfield.movingeyes.libraries.ui.components.color.parseHexOrNull
import com.dangerfield.movingeyes.libraries.ui.components.color.toHexDigits
import com.dangerfield.movingeyes.libraries.ui.components.text.OutlinedTextField
import com.dangerfield.movingeyes.libraries.ui.components.text.Text
import com.dangerfield.movingeyes.system.AppTheme
import com.dangerfield.movingeyes.system.Dimension
import org.jetbrains.compose.ui.tooling.preview.Preview

private val SwatchCornerRadius = 10.dp
private val SwatchSize = 40.dp

/**
 * Sclera, iris, pupil, canvas: a picker for browsing, a hex field for knowing.
 *
 * **Both, not one.** A picker alone is unusable with low vision and is the
 * wrong tool when someone is matching a colour they already have — a paint
 * chip, a costume, a photo of a cat — which is a hex code. A hex field alone
 * makes finding a colour you *don't* already know a guessing game.
 *
 * There is deliberately no eyedropper: it would mean a camera or a screen
 * capture, and the app asks for exactly one permission.
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

    // Held rather than derived from `color` so dragging to black or to zero
    // saturation doesn't lose the hue and snap the slider back to red.
    var hsv by remember { mutableStateOf(Hsv.from(color)) }
    if (hsv.toColor() != color) hsv = Hsv.from(color)

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

        ColorPicker(
            hsv = hsv,
            onHsvChange = {
                hsv = it
                onColorChange(it.toColor())
            },
        )

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

/** Null until all six digits are present, so a partial entry can't repaint the canvas. */

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
