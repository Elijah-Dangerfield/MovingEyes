package com.dangerfield.movingeyes.libraries.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.dangerfield.movingeyes.libraries.ui.PreviewContent
import com.dangerfield.movingeyes.libraries.ui.components.icon.Icon
import com.dangerfield.movingeyes.libraries.ui.components.icon.IconSize
import com.dangerfield.movingeyes.libraries.ui.components.icon.Icons
import com.dangerfield.movingeyes.libraries.ui.components.text.Text
import com.dangerfield.movingeyes.system.AppTheme
import com.dangerfield.movingeyes.system.Dimension
import org.jetbrains.compose.ui.tooling.preview.Preview

private val BadgeCornerRadius = 8.dp
private val BadgeSize = 24.dp

/**
 * The padlock on a paid style, mood or control.
 *
 * The badge marks a thing as paid; it never dims or disables it. Every locked
 * control in this app stays **visible and animating** — a locked eye style
 * blinks in the picker the whole time you're browsing, and tapping it runs it
 * live on your own eyes for thirty seconds. That's the entire paywall strategy:
 * motion is the thing a screenshot can't show a friend, so the only way to sell
 * it is to let people watch it.
 *
 * So: amber on a dark wash, small, in a corner. It should read as "this is
 * more" and never as "this is off".
 */
@Composable
fun LockBadge(
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    Box(
        modifier = modifier
            .size(BadgeSize)
            .clip(RoundedCornerShape(BadgeCornerRadius))
            .background(AppTheme.colors.surfacePrimary.color.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon = Icons.Lock(contentDescription),
            size = IconSize.Small,
            color = AppTheme.colors.accentPrimary,
        )
    }
}

/**
 * The inline version, for a row rather than a corner: a padlock and a price.
 * Used on list rows in Settings and on the Motion tab header.
 */
@Composable
fun LockLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(BadgeCornerRadius))
            .background(AppTheme.colors.accentSecondary.color.copy(alpha = 0.35f))
            .padding(horizontal = Dimension.D400, vertical = Dimension.D200),
        horizontalArrangement = Arrangement.spacedBy(Dimension.D200),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon = Icons.Lock(contentDescription = null),
            size = IconSize.Small,
            color = AppTheme.colors.accentPrimary,
        )
        Text(
            text = text,
            typography = AppTheme.typography.Label.L400.SemiBold,
            color = AppTheme.colors.accentPrimary,
        )
    }
}

@Preview
@Composable
private fun PreviewLockBadge() {
    PreviewContent(modifier = Modifier.padding(Dimension.D800)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Dimension.D600),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LockBadge()
            LockLabel(text = "Unlock $4.99")
        }
    }
}
