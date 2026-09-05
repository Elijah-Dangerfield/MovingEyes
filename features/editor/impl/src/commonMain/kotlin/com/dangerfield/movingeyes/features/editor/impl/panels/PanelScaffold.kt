package com.dangerfield.movingeyes.features.editor.impl.panels

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import com.dangerfield.movingeyes.system.thenIfNotNull
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
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
    /**
     * Non-null when the row is paid and unowned. The content is then rendered
     * but sealed behind a single tap target, so a locked control can be read
     * and can't be operated. Anything else means a control that looks live,
     * moves under your finger, and does nothing — or worse, fires its "you
     * need to pay" route on every frame of the drag.
     */
    onLocked: (() -> Unit)? = null,
    /** Makes the whole row a target for whatever its trailing control does. A
     *  switch is a small thing to hit at arm's length, and the label beside it
     *  means the same thing. */
    onRowClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .thenIfNotNull(onRowClick) {
                clip(RoundedCornerShape(RowCornerRadius)).clickable(onClick = it)
            }
            .then(if (onLocked != null) Modifier.sealed(onLocked) else Modifier),
        verticalArrangement = Arrangement.spacedBy(Dimension.D300),
    ) {
        // The trailing control takes the leftover width rather than the row
        // splitting what's there: SegmentedControl asks for all of it, so under
        // SpaceBetween the gap collapsed and ROTATE sat flush against the
        // Group/Each border.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimension.D500),
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
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                trailing()
            }
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
    onLocked: (() -> Unit)? = null,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    PanelRow(
        label = label,
        modifier = modifier,
        isLocked = isLocked,
        onLocked = onLocked,
        trailing = {
            Text(
                text = valueLabel,
                typography = AppTheme.typography.Readout.R300,
                color = AppTheme.colors.textSecondary,
            )
        },
    ) {
        // Inset from the panel's own edges. The panel is already only 16dp off
        // the screen, and Android reserves roughly 20dp either side for the
        // back gesture — so a thumb at 0% or 100% sat inside the strip that
        // swipes you out of the app, and grabbing it navigated back instead.
        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SliderEdgeInset),
        )
    }
}

/**
 * Swallows every gesture over its content and turns the whole area into one
 * tap. Dimmed, because a control that can't be operated should not look like
 * one that can.
 */
private fun Modifier.sealed(onTap: () -> Unit): Modifier = this
    .alpha(SealedAlpha)
    .pointerInput(onTap) { detectTapGestures { onTap() } }

private const val SealedAlpha = 0.55f

/** Keeps a slider's travel clear of the system's edge-swipe zone. */
private val SliderEdgeInset = 12.dp

private val RowCornerRadius = 10.dp
