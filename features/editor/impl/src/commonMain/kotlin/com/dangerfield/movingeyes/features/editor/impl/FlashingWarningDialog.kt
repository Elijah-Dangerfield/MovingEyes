package com.dangerfield.movingeyes.features.editor.impl

import androidx.compose.runtime.Composable
import com.dangerfield.movingeyes.libraries.eyes.Mood
import com.dangerfield.movingeyes.libraries.ui.components.dialog.BasicDialog
import com.dangerfield.movingeyes.libraries.ui.components.dialog.rememberDialogState
import com.dangerfield.movingeyes.libraries.ui.components.button.Button
import com.dangerfield.movingeyes.libraries.ui.components.button.ButtonSize
import com.dangerfield.movingeyes.libraries.ui.components.button.ButtonStyle
import com.dangerfield.movingeyes.libraries.ui.components.text.Text
import com.dangerfield.movingeyes.system.AppTheme
import movingeyes.libraries.resources.generated.resources.Res
import movingeyes.libraries.resources.generated.resources.flashing_body
import movingeyes.libraries.resources.generated.resources.flashing_continue
import movingeyes.libraries.resources.generated.resources.flashing_reduce
import movingeyes.libraries.resources.generated.resources.flashing_title
import org.jetbrains.compose.resources.stringResource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import com.dangerfield.movingeyes.system.Dimension

/**
 * Shown once per strobing mood, before it runs. It names the mood rather than
 * warning generically, and offers Reduce flashing inline — someone who needs
 * the setting shouldn't have to go and find it after being startled.
 */
@Composable
fun FlashingWarningDialog(
    mood: Mood,
    onContinue: () -> Unit,
    onReduceFlashing: () -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberDialogState()

    BasicDialog(
        state = state,
        onDismissRequest = onDismiss,
        topContent = {
            Text(
                text = stringResource(Res.string.flashing_title),
                typography = AppTheme.typography.Heading.H700,
            )
        },
        content = {
            Text(
                text = stringResource(Res.string.flashing_body, stringResource(mood.label)),
                typography = AppTheme.typography.Body.B600,
                color = AppTheme.colors.textSecondary,
            )
        },
        bottomContent = {
            Column {
                Button(
                    onClick = onReduceFlashing,
                    size = ButtonSize.Medium,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(Res.string.flashing_reduce))
                }
                Spacer(modifier = Modifier.height(Dimension.D400))
                Button(
                    onClick = onContinue,
                    size = ButtonSize.Medium,
                    style = ButtonStyle.Text,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(Res.string.flashing_continue))
                }
            }
        },
    )
}
