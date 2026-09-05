package com.dangerfield.movingeyes.features.editor.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dangerfield.movingeyes.libraries.ui.components.button.Button
import com.dangerfield.movingeyes.libraries.ui.components.button.ButtonSize
import com.dangerfield.movingeyes.libraries.ui.components.button.ButtonStyle
import com.dangerfield.movingeyes.libraries.ui.components.dialog.BasicDialog
import com.dangerfield.movingeyes.libraries.ui.components.dialog.rememberDialogState
import com.dangerfield.movingeyes.libraries.ui.components.text.Text
import com.dangerfield.movingeyes.system.AppTheme
import com.dangerfield.movingeyes.system.Dimension
import movingeyes.libraries.resources.generated.resources.Res
import movingeyes.libraries.resources.generated.resources.locked_unlock
import movingeyes.libraries.resources.generated.resources.reactivity_not_now
import org.jetbrains.compose.resources.stringResource

/**
 * What a locked control opens instead of a full-screen paywall.
 *
 * You tapped Demon, so you see Demon — running, at a size you can actually
 * judge — and then choose. Jumping straight to a generic paywall throws away
 * the thing you were reaching for, which is the only reason you were interested.
 */
@Composable
fun LockedPreviewSheet(
    title: String,
    onUnlock: () -> Unit,
    onDismiss: () -> Unit,
    preview: @Composable () -> Unit,
) {
    val state = rememberDialogState()

    BasicDialog(
        state = state,
        onDismissRequest = onDismiss,
        topContent = {
            Text(text = title, typography = AppTheme.typography.Heading.H700)
        },
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimension.D400)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(PreviewHeight)
                        .clip(RoundedCornerShape(PreviewCorner))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center,
                ) {
                    preview()
                }
            }
        },
        bottomContent = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimension.D400)) {
                Button(
                    onClick = onUnlock,
                    size = ButtonSize.Medium,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(Res.string.locked_unlock))
                }
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

/** The preview is the whole pitch now that there is no trial, so it gets the
 *  room a thing being judged deserves. */
private val PreviewHeight = 260.dp
private val PreviewCorner = 12.dp
