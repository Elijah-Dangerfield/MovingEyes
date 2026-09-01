package com.dangerfield.movingeyes.libraries.ui.components.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.dangerfield.movingeyes.system.AppTheme
import com.dangerfield.movingeyes.system.Dimension
import com.dangerfield.movingeyes.system.VerticalSpacerD500
import com.dangerfield.movingeyes.libraries.ui.PreviewContent
import com.dangerfield.movingeyes.libraries.ui.components.button.Button
import com.dangerfield.movingeyes.libraries.ui.components.button.ButtonSize
import com.dangerfield.movingeyes.libraries.ui.components.button.ButtonStyle
import com.dangerfield.movingeyes.libraries.ui.components.button.ButtonType
import com.dangerfield.movingeyes.libraries.ui.components.text.Text
import movingeyes.libraries.resources.generated.resources.Res
import movingeyes.libraries.resources.generated.resources.error_dismiss
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/** One entry in the shake menu. Dev-facing, so labels are plain strings. */
data class ShakeAction(
    val label: String,
    val onSelect: () -> Unit,
    val type: ButtonType = ButtonType.Secondary,
)

/**
 * The debug menu, reached by shaking the device. Shake only arms in debug
 * builds — a tablet being taped behind cardboard gets shaken plenty, and a
 * dialog over a mounted scene would be worse than useless.
 */
@Composable
fun ShakeDialog(
    headline: String,
    subtext: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    state: DialogState = rememberDialogState(),
    actions: List<ShakeAction> = emptyList(),
) {
    BasicDialog(
        state = state,
        onDismissRequest = onDismiss,
        modifier = modifier,
        topContent = {
            Text(
                text = headline,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        content = {
            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (subtext != null) {
                    Spacer(modifier = Modifier.height(Dimension.D300))
                    Text(
                        text = subtext,
                        typography = AppTheme.typography.Body.B600,
                        color = AppTheme.colors.textSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    VerticalSpacerD500()
                }
            }
        },
        bottomContent = {
            Column {
                actions.forEach { action ->
                    Button(
                        onClick = {
                            state.dismiss()
                            action.onSelect()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        size = ButtonSize.Medium,
                        type = action.type,
                    ) {
                        Text(action.label)
                    }
                    Spacer(modifier = Modifier.height(Dimension.D500))
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    size = ButtonSize.Medium,
                    style = ButtonStyle.Text
                ) {
                    Text(stringResource(Res.string.error_dismiss))
                }
            }
        }
    )
}

@Preview
@Composable
private fun ShakeDialogPreview_WithSubtext() {
    PreviewContent {
        ShakeDialog(
            headline = "I felt that.",
            subtext = "Testing the waters?",
            onDismiss = {},
        )
    }
}

@Preview
@Composable
private fun ShakeDialogPreview_NoSubtext() {
    PreviewContent {
        ShakeDialog(
            headline = "Whoa.",
            subtext = null,
            onDismiss = {},
        )
    }
}

@Preview
@Composable
private fun ShakeDialogPreview_WithActions() {
    PreviewContent {
        ShakeDialog(
            headline = "I felt that.",
            subtext = "Testing the waters?",
            onDismiss = {},
            actions = listOf(ShakeAction(label = "Design system", onSelect = {})),
        )
    }
}
