package com.dangerfield.movingeyes.features.editor.impl.panels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dangerfield.movingeyes.libraries.ui.components.LockBadge
import com.dangerfield.movingeyes.libraries.ui.components.Slider
import com.dangerfield.movingeyes.libraries.ui.components.text.Text
import com.dangerfield.movingeyes.system.AppTheme
import com.dangerfield.movingeyes.system.Dimension

enum class PanelTab { Place, Look, Motion, Scene }

/**
 * [isLocked] shows the badge but disables nothing: tapping a paid control is
 * what starts the demo, so disabling it would make the demo unreachable.
 */
@Composable
fun PanelRow(
    label: String,
    modifier: Modifier = Modifier,
    isLocked: Boolean = false,
    trailing: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimension.D300),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Dimension.D300),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    typography = AppTheme.typography.Caption.C300,
                    color = AppTheme.colors.textTertiary,
                    allCaps = true,
                )
                if (isLocked) LockBadge()
            }
            trailing()
        }
        content()
    }
}

/** Value shown in the mono readout face. */
@Composable
fun PanelSlider(
    label: String,
    value: Float,
    valueLabel: String,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    isLocked: Boolean = false,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    PanelRow(
        label = label,
        modifier = modifier,
        isLocked = isLocked,
        trailing = {
            Text(
                text = valueLabel,
                typography = AppTheme.typography.Readout.R300,
                color = AppTheme.colors.textSecondary,
            )
        },
    ) {
        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
