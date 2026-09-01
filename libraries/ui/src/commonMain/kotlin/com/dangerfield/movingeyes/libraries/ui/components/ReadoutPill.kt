package com.dangerfield.movingeyes.libraries.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.dangerfield.movingeyes.libraries.ui.PreviewContent
import com.dangerfield.movingeyes.libraries.ui.components.text.Text
import com.dangerfield.movingeyes.libraries.ui.system.color.ColorResource
import com.dangerfield.movingeyes.system.AppTheme
import com.dangerfield.movingeyes.system.Dimension
import org.jetbrains.compose.ui.tooling.preview.Preview

private val PillCornerRadius = 9.dp

/**
 * The live measurement chip: `2 eyes · 148 px · 31.3 mm · 0°`.
 *
 * Three things make this work and all three are easy to lose in a refactor:
 *
 *  - **Mono.** The value updates continuously while a finger is down. In a
 *    proportional face the pill would breathe as digits changed width; in mono
 *    it sits still, which is the difference between an instrument and a jitter.
 *  - **Amber.** This is a live value, and live is what the accent means.
 *  - **On a plate, not on the canvas.** It floats over true black, so it needs
 *    its own surface or it reads as debug text someone forgot to delete.
 *
 * Use ` · ` between fields; the separator is part of the look.
 */
@Composable
fun ReadoutPill(
    text: String,
    modifier: Modifier = Modifier,
    emphasized: Boolean = true,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(PillCornerRadius))
            .background(AppTheme.colors.surfacePrimary.color.copy(alpha = 0.9f))
            .border(1.dp, AppTheme.colors.border.color, RoundedCornerShape(PillCornerRadius))
            .padding(horizontal = Dimension.D500, vertical = Dimension.D300),
    ) {
        Text(
            text = text,
            typography = AppTheme.typography.Readout.R400,
            color = if (emphasized) AppTheme.colors.accentPrimary else AppTheme.colors.textSecondary,
        )
    }
}

/**
 * A labelled numeric field for the transform row — `X` above `597`.
 *
 * The label is sans and quiet, the value is mono and loud. Same reasoning as
 * [ReadoutPill]: a value that changes under a drag must not move its own label.
 */
@Composable
fun ReadoutField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: ColorResource = AppTheme.colors.text,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Dimension.D100),
    ) {
        Text(
            text = label,
            typography = AppTheme.typography.Caption.C300,
            color = AppTheme.colors.textTertiary,
        )
        Text(
            text = value,
            typography = AppTheme.typography.Readout.R500,
            color = valueColor,
        )
    }
}

@Preview
@Composable
private fun PreviewReadout() {
    PreviewContent(modifier = Modifier.padding(Dimension.D800)) {
        Column(verticalArrangement = Arrangement.spacedBy(Dimension.D700)) {
            ReadoutPill(text = "2 eyes · 148 px · 31.3 mm · 0°")
            ReadoutPill(text = "Snapped to centre and to 46.6 mm")
            ReadoutPill(text = "17% · plug in soon", emphasized = false)
            Column(verticalArrangement = Arrangement.spacedBy(Dimension.D500)) {
                ReadoutField(label = "X", value = "597")
                ReadoutField(label = "Y", value = "384")
                ReadoutField(label = "IPD", value = "220 px")
            }
        }
    }
}
