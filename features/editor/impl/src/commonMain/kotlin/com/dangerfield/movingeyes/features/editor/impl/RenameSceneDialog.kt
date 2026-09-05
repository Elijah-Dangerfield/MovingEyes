package com.dangerfield.movingeyes.features.editor.impl

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.dangerfield.movingeyes.libraries.ui.components.button.Button
import com.dangerfield.movingeyes.libraries.ui.components.button.ButtonSize
import com.dangerfield.movingeyes.libraries.ui.components.button.ButtonStyle
import com.dangerfield.movingeyes.libraries.ui.components.dialog.BasicDialog
import com.dangerfield.movingeyes.libraries.ui.components.dialog.rememberDialogState
import com.dangerfield.movingeyes.libraries.ui.components.text.OutlinedTextField
import com.dangerfield.movingeyes.libraries.ui.components.text.Text
import com.dangerfield.movingeyes.system.AppTheme
import movingeyes.libraries.resources.generated.resources.Res
import movingeyes.libraries.resources.generated.resources.scene_rename_cancel
import movingeyes.libraries.resources.generated.resources.scene_rename_save
import movingeyes.libraries.resources.generated.resources.scene_rename_title
import org.jetbrains.compose.resources.stringResource

/**
 * Renaming, one tap from the name itself.
 *
 * Opens focused, and opens *empty* when the scene is still on its placeholder
 * name — which is the common case, since that is why anyone taps it. Typing is
 * then the only thing left to do, rather than tap, select all, delete, type. A
 * scene with a real name is pre-filled, because then you are editing rather
 * than replacing.
 *
 * There was nowhere else in the app to do this, which is how every scene ended
 * up called the same thing.
 */
@Composable
fun RenameSceneDialog(
    currentName: String,
    placeholderName: String,
    onRename: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberDialogState()
    val focus = remember { FocusRequester() }
    var value by remember {
        mutableStateOf(if (currentName == placeholderName) "" else currentName)
    }
    val trimmed = value.trim()

    val keyboard = LocalSoftwareKeyboardController.current

    // Keyed on the field being *placed*, not on entering composition. The
    // dialog registers its content with a host that renders it a frame or more
    // later, so asking for focus from this function's body asked before the
    // field existed and silently did nothing. Focus alone also leaves the
    // keyboard down on Android, so both are requested here.
    var isFieldPlaced by remember { mutableStateOf(false) }
    LaunchedEffect(isFieldPlaced) {
        if (!isFieldPlaced) return@LaunchedEffect
        focus.requestFocus()
        keyboard?.show()
    }

    BasicDialog(
        state = state,
        onDismissRequest = onDismiss,
        topContent = {
            Text(
                text = stringResource(Res.string.scene_rename_title),
                typography = AppTheme.typography.Heading.H700,
            )
        },
        content = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                placeholder = { Text(placeholderName) },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focus)
                    .onGloballyPositioned { isFieldPlaced = true },
            )
        },
        bottomContent = {
            Button(
                onClick = {
                    keyboard?.hide()
                    onRename(trimmed)
                },
                enabled = trimmed.isNotEmpty(),
                size = ButtonSize.Medium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(Res.string.scene_rename_save))
            }
            Button(
                onClick = onDismiss,
                size = ButtonSize.Medium,
                style = ButtonStyle.Text,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(Res.string.scene_rename_cancel))
            }
        },
    )
}
