package com.dangerfield.movingeyes.libraries.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.dangerfield.movingeyes.libraries.ui.PreviewContent
import com.dangerfield.movingeyes.libraries.ui.components.text.Text
import com.dangerfield.movingeyes.system.AppTheme
import com.dangerfield.movingeyes.system.Dimension
import com.dangerfield.movingeyes.system.Motion
import kotlinx.coroutines.delay
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private val BarCornerRadius = 12.dp

/** An action rendered at the trailing edge of a [ToastBar]. */
data class ToastAction(val label: String, val onSelect: () -> Unit)

/**
 * A non-blocking bar that says what just happened and offers a way back.
 *
 * Three places use it, and they share one rule: **never dim the screen and
 * never take focus.** These appear over a canvas someone has spent four minutes
 * aligning, sometimes over a scene already mounted behind cardboard. A modal
 * here would be worse than saying nothing.
 *
 *  - After a device rotation: "Kept your sizes." with Refit and Undo.
 *  - When a 30-second demo expires: "Back to the free motion." with Keep.
 *  - Low battery in display mode, which is [ToastBar] with no actions at all.
 *
 * [autoDismissAfter] runs a timer; pass `null` for a bar the caller dismisses.
 */
@Composable
fun ToastBar(
    visible: Boolean,
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    actions: List<ToastAction> = emptyList(),
    autoDismissAfter: Duration? = Motion.Notice.RotationToastSeconds.seconds,
) {
    LaunchedEffect(visible, autoDismissAfter) {
        if (!visible || autoDismissAfter == null) return@LaunchedEffect
        delay(autoDismissAfter)
        onDismiss()
    }

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(Motion.Chrome.restore()) +
            slideInVertically(Motion.Chrome.restore()) { it / 2 },
        exit = fadeOut(Motion.Chrome.restore()) +
            slideOutVertically(Motion.Chrome.restore()) { it / 2 },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(BarCornerRadius))
                .background(AppTheme.colors.surfacePrimary.color)
                .border(1.dp, AppTheme.colors.border.color, RoundedCornerShape(BarCornerRadius))
                .padding(horizontal = Dimension.D700, vertical = Dimension.D500),
            horizontalArrangement = Arrangement.spacedBy(Dimension.D700),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Dimension.D100),
            ) {
                Text(text = message, typography = AppTheme.typography.Label.L500.SemiBold)
                if (supporting != null) {
                    Text(
                        text = supporting,
                        typography = AppTheme.typography.Caption.C400,
                        color = AppTheme.colors.textSecondary,
                    )
                }
            }

            actions.forEach { action ->
                Text(
                    text = action.label,
                    typography = AppTheme.typography.Label.L500.SemiBold,
                    color = AppTheme.colors.accentPrimary,
                    modifier = Modifier
                        .clickable(onClick = action.onSelect)
                        .padding(vertical = Dimension.D300, horizontal = Dimension.D200),
                )
            }
        }
    }
}

@Preview
@Composable
private fun PreviewToastBar() {
    PreviewContent(modifier = Modifier.padding(Dimension.D800)) {
        Column(verticalArrangement = Arrangement.spacedBy(Dimension.D700)) {
            ToastBar(
                visible = true,
                message = "Kept your sizes.",
                onDismiss = {},
                actions = listOf(ToastAction("Refit") {}, ToastAction("Undo") {}),
                autoDismissAfter = null,
            )
            ToastBar(
                visible = true,
                message = "Demo ended",
                supporting = "Back to the free motion.",
                onDismiss = {},
                actions = listOf(ToastAction("Keep Frantic") {}),
                autoDismissAfter = null,
            )
            ToastBar(
                visible = true,
                message = "17% · plug in soon",
                onDismiss = {},
                autoDismissAfter = null,
            )
        }
    }
}
