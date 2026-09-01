package com.dangerfield.movingeyes.libraries.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.dangerfield.movingeyes.libraries.ui.PreviewContent
import com.dangerfield.movingeyes.libraries.ui.components.text.Text
import com.dangerfield.movingeyes.libraries.ui.system.color.ColorResource
import com.dangerfield.movingeyes.system.AppTheme
import com.dangerfield.movingeyes.system.Dimension
import com.dangerfield.movingeyes.system.Motion
import com.dangerfield.movingeyes.system.Target
import org.jetbrains.compose.ui.tooling.preview.Preview

private val TrackPadding = 4.dp
private val SegmentCornerRadius = 9.dp
private val TrackCornerRadius = 12.dp

/**
 * A row of mutually-exclusive options in one track: the editor's
 * Place / Look / Motion / Scene tabs, and Group / Each on the transform row.
 *
 * The selected segment lifts to [Colors.surfaceTertiary]; the rest stay flat on
 * the track. That's deliberately quieter than an amber pill per tab — the
 * accent has to keep meaning "live", and on the editor screen it belongs to
 * snap guides and the readout, not to which tab you're on.
 *
 * The track is [Target.Minimum] tall so each segment clears the touch minimum,
 * which holds up to four options. Don't put five in.
 */
@Composable
fun <T> SegmentedControl(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    label: (T) -> String,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(Target.Minimum)
            .clip(RoundedCornerShape(TrackCornerRadius))
            .background(AppTheme.colors.surfaceSecondary.color)
            .border(1.dp, AppTheme.colors.border.color, RoundedCornerShape(TrackCornerRadius))
            .padding(TrackPadding),
        horizontalArrangement = Arrangement.spacedBy(TrackPadding),
    ) {
        options.forEach { option ->
            Segment(
                label = label(option),
                isSelected = option == selected,
                onClick = { onSelect(option) },
            )
        }
    }
}

@Composable
private fun RowScope.Segment(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val background by animateColorAsState(
        targetValue = if (isSelected) {
            AppTheme.colors.surfaceTertiary.color
        } else {
            AppTheme.colors.surfaceSecondary.color
        },
        animationSpec = Motion.Panel.contentFade(),
        label = "segmentBackground",
    )
    val foreground by animateColorAsState(
        targetValue = if (isSelected) {
            AppTheme.colors.text.color
        } else {
            AppTheme.colors.textSecondary.color
        },
        animationSpec = Motion.Panel.contentFade(),
        label = "segmentForeground",
    )

    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clip(RoundedCornerShape(SegmentCornerRadius))
            .background(background)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            typography = AppTheme.typography.Label.L500.SemiBold,
            color = ColorResource.FromColor(foreground, "segment-foreground"),
        )
    }
}

@Preview
@Composable
private fun PreviewSegmentedControl() {
    PreviewContent(modifier = Modifier.padding(Dimension.D800)) {
        Column(verticalArrangement = Arrangement.spacedBy(Dimension.D700)) {
            SegmentedControl(
                options = listOf("Place", "Look", "Motion", "Scene"),
                selected = "Look",
                onSelect = {},
                label = { it },
            )
            SegmentedControl(
                options = listOf("Group", "Each"),
                selected = "Group",
                onSelect = {},
                label = { it },
            )
        }
    }
}
