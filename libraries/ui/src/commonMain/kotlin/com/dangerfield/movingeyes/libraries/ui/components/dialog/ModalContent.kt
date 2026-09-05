package com.dangerfield.movingeyes.libraries.ui.components.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dangerfield.movingeyes.libraries.ui.system.LocalContentColor
import com.dangerfield.movingeyes.system.thenIfNotNull
import com.dangerfield.movingeyes.system.AppTheme
import com.dangerfield.movingeyes.system.Dimension
import com.dangerfield.movingeyes.system.color.ProvideContentColor
import com.dangerfield.movingeyes.libraries.ui.PreviewContent
import com.dangerfield.movingeyes.libraries.ui.system.color.ColorResource
import com.dangerfield.movingeyes.libraries.ui.components.button.Button
import com.dangerfield.movingeyes.libraries.ui.components.button.ButtonSize
import com.dangerfield.movingeyes.libraries.ui.components.button.ProvideButtonConfig
import com.dangerfield.movingeyes.libraries.ui.components.text.ProvideTextConfig
import com.dangerfield.movingeyes.libraries.ui.components.text.Text
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun ModalContent(
    modifier: Modifier = Modifier,
    /**
     * Null by default: the dialog or sheet around this already painted a
     * surface, and painting a second one inside it is how the dialog ended up
     * a near-black panel inside a grey ring. Pass one only when this is used
     * somewhere that hasn't got a surface of its own.
     */
    backgroundColor: ColorResource? = null,
    /** Inherits whatever the surrounding surface provided, so the pairing can't
     *  drift. See `Colors.contentColorFor`. */
    contentColor: ColorResource = LocalContentColor.current,
    topContent: @Composable () -> Unit = {},
    content: @Composable () -> Unit = {},
    bottomContent: @Composable (() -> Unit)? = null,
) {

    Column(
        modifier = modifier.thenIfNotNull(backgroundColor) { background(it.color) }
    ) {

        ProvideContentColor(color = contentColor) {

            ProvideTextConfig(AppTheme.typography.Display.D1000) {
                topContent()
            }

            Spacer(modifier = Modifier.height(Dimension.D600))

            ProvideTextConfig(AppTheme.typography.Body.B700) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)

                ) {
                    content()
                }
            }

            if (bottomContent != null) {
                Spacer(modifier = Modifier.height(Dimension.D1000))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .thenIfNotNull(backgroundColor) { background(it.color) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ProvideButtonConfig(size = ButtonSize.Small) {
                        bottomContent()
                    }
                }

                Spacer(modifier = Modifier.height(Dimension.D800))
            }
        }
    }
}

@Composable
@Preview
private fun PreviewModalContent() {
    PreviewContent {
        ModalContent(
            modifier = Modifier,
            topContent = { Text(text = "Top Content") },
            content = {
                Column {
                    Text(text = "context".repeat(50))
                }
            },
            bottomContent = {
                Button(onClick = { }) {
                    Text(text = "Bottom Content")
                }
            },
        )
    }
}

@Composable
@Preview
private fun PreviewModalContentLong() {
    PreviewContent {
        ModalContent(
            modifier = Modifier,
            topContent = { Text(text = "Top Content") },
            content = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text(text = "This is a bunch of words that take sus space".repeat(100))
                }
            },
            bottomContent = {
                Button(onClick = { }) {
                    Text(text = "Bottom Content")
                }
            },
        )
    }
}