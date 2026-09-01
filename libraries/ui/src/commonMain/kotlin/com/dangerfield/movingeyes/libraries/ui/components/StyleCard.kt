package com.dangerfield.movingeyes.libraries.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dangerfield.movingeyes.libraries.ui.PreviewContent
import com.dangerfield.movingeyes.libraries.ui.components.text.Text
import com.dangerfield.movingeyes.system.AppTheme
import com.dangerfield.movingeyes.system.Dimension
import com.dangerfield.movingeyes.system.Motion
import org.jetbrains.compose.ui.tooling.preview.Preview

private val CardCornerRadius = 12.dp
private val CardWidth = 96.dp

/**
 * One entry in the eye-style picker.
 *
 * **The preview must be alive.** Not a screenshot, not a static drawing — an
 * actual eye running the actual renderer, blinking and looking around in the
 * picker while you browse. Two reasons, and neither is decoration:
 *
 *  - Ten of the twelve styles are paid, and motion is the thing a screenshot
 *    cannot show a friend. If the locked cards are still images, the paywall is
 *    selling something invisible.
 *  - A style is a parameter set, not an asset. A static thumbnail would be a
 *    second source of truth that drifts the moment anyone tunes a gradient.
 *
 * [preview] is a slot so this component can live in the design system without
 * depending on the renderer. The picker passes the real eye; the previews here
 * pass a placeholder disc.
 *
 * A locked card is **never dimmed**. It carries a [LockBadge] and keeps
 * animating at full strength.
 */
@Composable
fun StyleCard(
    label: String,
    isSelected: Boolean,
    isLocked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    preview: @Composable () -> Unit,
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) {
            AppTheme.colors.accentPrimary.color
        } else {
            AppTheme.colors.border.color
        },
        animationSpec = Motion.Panel.contentFade(),
        label = "styleCardBorder",
    )

    Column(
        modifier = modifier.width(CardWidth),
        verticalArrangement = Arrangement.spacedBy(Dimension.D300),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(CardCornerRadius))
                // True black behind the eye, matching the canvas — a style has
                // to be judged against the surface it will actually sit on.
                .background(AppTheme.colors.background.color)
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(CardCornerRadius),
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            preview()

            if (isLocked) {
                LockBadge(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(Dimension.D200),
                )
            }
        }

        Text(
            text = label,
            typography = AppTheme.typography.Label.L400,
            color = if (isSelected) AppTheme.colors.text else AppTheme.colors.textSecondary,
        )
    }
}

@Preview
@Composable
private fun PreviewStyleCard() {
    PreviewContent(modifier = Modifier.padding(Dimension.D800)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Dimension.D500)) {
            StyleCard(label = "Cartoon", isSelected = true, isLocked = false, onClick = {}) {
                PlaceholderEye(Color(0xFF4FA3C7))
            }
            StyleCard(label = "Human", isSelected = false, isLocked = false, onClick = {}) {
                PlaceholderEye(Color(0xFF7A5A34))
            }
            StyleCard(label = "Feline", isSelected = false, isLocked = true, onClick = {}) {
                PlaceholderEye(Color(0xFFC8D24A))
            }
        }
    }
}

/** Stands in for the renderer in previews. Replaced by the real eye in the picker. */
@Composable
private fun PlaceholderEye(iris: Color) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(iris),
    )
}
