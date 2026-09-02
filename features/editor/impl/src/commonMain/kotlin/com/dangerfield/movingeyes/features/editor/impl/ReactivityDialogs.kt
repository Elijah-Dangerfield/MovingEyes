package com.dangerfield.movingeyes.features.editor.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dangerfield.movingeyes.libraries.ui.components.button.Button
import com.dangerfield.movingeyes.libraries.ui.components.button.ButtonSize
import com.dangerfield.movingeyes.libraries.ui.components.button.ButtonStyle
import com.dangerfield.movingeyes.libraries.ui.components.dialog.BasicDialog
import com.dangerfield.movingeyes.libraries.ui.components.dialog.rememberDialogState
import com.dangerfield.movingeyes.libraries.ui.components.text.Text
import com.dangerfield.movingeyes.system.AppTheme
import com.dangerfield.movingeyes.system.Dimension
import movingeyes.libraries.resources.generated.resources.Res
import movingeyes.libraries.resources.generated.resources.reactivity_allow
import movingeyes.libraries.resources.generated.resources.reactivity_body
import movingeyes.libraries.resources.generated.resources.reactivity_denied
import movingeyes.libraries.resources.generated.resources.reactivity_denied_settings
import movingeyes.libraries.resources.generated.resources.reactivity_not_now
import movingeyes.libraries.resources.generated.resources.reactivity_privacy
import movingeyes.libraries.resources.generated.resources.reactivity_title
import org.jetbrains.compose.resources.stringResource

/**
 * Shown before the OS prompt, never after.
 *
 * The system dialog gives one line and no way back — someone who taps Deny
 * there has to find Settings to change their mind. Explaining first, in our own
 * words, is also the only place the privacy promise can be made where it will
 * actually be read.
 */
@Composable
fun MicrophoneExplanationDialog(
    onAllow: () -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberDialogState()

    BasicDialog(
        state = state,
        onDismissRequest = onDismiss,
        topContent = {
            Text(
                text = stringResource(Res.string.reactivity_title),
                typography = AppTheme.typography.Heading.H700,
            )
        },
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimension.D400)) {
                Text(
                    text = stringResource(Res.string.reactivity_body),
                    typography = AppTheme.typography.Body.B600,
                    color = AppTheme.colors.textSecondary,
                )
                Text(
                    text = stringResource(Res.string.reactivity_privacy),
                    typography = AppTheme.typography.Body.B600,
                    color = AppTheme.colors.textSecondary,
                )
            }
        },
        bottomContent = {
            Column {
                Button(
                    onClick = onAllow,
                    size = ButtonSize.Medium,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(Res.string.reactivity_allow))
                }
                Spacer(modifier = Modifier.height(Dimension.D400))
                Button(
                    onClick = onDismiss,
                    size = ButtonSize.Medium,
                    style = ButtonStyle.Text,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(Res.string.reactivity_not_now))
                }
            }
        },
    )
}

/** Denial is not a dead end: everything else runs, and this only offers the way
 *  back rather than insisting on it. */
@Composable
fun MicrophoneDeniedDialog(
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberDialogState()

    BasicDialog(
        state = state,
        onDismissRequest = onDismiss,
        topContent = {
            Text(
                text = stringResource(Res.string.reactivity_title),
                typography = AppTheme.typography.Heading.H700,
            )
        },
        content = {
            Text(
                text = stringResource(Res.string.reactivity_denied),
                typography = AppTheme.typography.Body.B600,
                color = AppTheme.colors.textSecondary,
            )
        },
        bottomContent = {
            Column {
                Button(
                    onClick = onOpenSettings,
                    size = ButtonSize.Medium,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(Res.string.reactivity_denied_settings))
                }
                Spacer(modifier = Modifier.height(Dimension.D400))
                Button(
                    onClick = onDismiss,
                    size = ButtonSize.Medium,
                    style = ButtonStyle.Text,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(Res.string.reactivity_not_now))
                }
            }
        },
    )
}
