package com.dangerfield.movingeyes.libraries.ui.components.text

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.dangerfield.movingeyes.system.AppTheme
import com.dangerfield.movingeyes.system.typography.TypographyResource
import com.dangerfield.movingeyes.libraries.ui.PreviewContent
import com.dangerfield.movingeyes.libraries.ui.makeBold
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun BoldPrefixedText(
    modifier: Modifier = Modifier,
    boldText: String,
    regularText: String,
    textAlign: TextAlign = TextAlign.Start,
    typography: TypographyResource = AppTheme.typography.Body.B700
) {
    val string = "$boldText $regularText".makeBold(boldText)

    Row(modifier = modifier) {
       Text(
           textAlign = textAlign,
           text = string,
           typography = typography
       )
    }
}

@Preview
@Composable
fun BoldPrefixedTextPreview() {
    PreviewContent {
        com.dangerfield.movingeyes.libraries.ui.components.text.BoldPrefixedText(
            boldText = "Role: ",
            regularText = "Spy",
            typography = AppTheme.typography.Body.B700
        )
    }
}