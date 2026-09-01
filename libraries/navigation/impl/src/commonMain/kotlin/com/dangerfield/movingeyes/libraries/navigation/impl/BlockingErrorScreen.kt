package com.dangerfield.movingeyes.libraries.navigation.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.text.style.TextAlign
import com.dangerfield.movingeyes.libraries.ui.PreviewContent
import com.dangerfield.movingeyes.libraries.ui.components.Screen
import com.dangerfield.movingeyes.libraries.ui.components.button.Button
import com.dangerfield.movingeyes.libraries.ui.components.button.ButtonSize
import com.dangerfield.movingeyes.libraries.ui.components.text.Text
import com.dangerfield.movingeyes.libraries.core.doNothing
import com.dangerfield.movingeyes.system.AppTheme
import movingeyes.libraries.resources.generated.resources.Res
import movingeyes.libraries.resources.generated.resources.error_code
import movingeyes.libraries.resources.generated.resources.error_report
import movingeyes.libraries.resources.generated.resources.error_report_aside
import org.jetbrains.compose.resources.stringResource
import com.dangerfield.movingeyes.system.Dimension
import com.dangerfield.movingeyes.system.VerticalSpacerD1000
import com.dangerfield.movingeyes.system.VerticalSpacerD1200
import com.dangerfield.movingeyes.system.VerticalSpacerD1600
import com.dangerfield.movingeyes.system.VerticalSpacerD500
import org.jetbrains.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun BlockingErrorScreen(
    title: String,
    subtitle: String,
    errorCode: Int?,
    onReportToDevelopers: (() -> Unit)? = null,
) {
    BackHandler { doNothing() }
    Screen { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = Dimension.D1000),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {

            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = title,
                typography = AppTheme.typography.Display.D1000,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(Dimension.D500))
            Text(
                text = subtitle,
                typography = AppTheme.typography.Body.B400,
                textAlign = TextAlign.Center,
            )
            errorCode?.let {
                Spacer(modifier = Modifier.height(Dimension.D400))
                Text(
                    text = stringResource(Res.string.error_code, it),
                    typography = AppTheme.typography.Body.B500,
                    color = AppTheme.colors.textSecondary,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(modifier = Modifier.weight(2f))

            if (onReportToDevelopers != null) {
                Button(
                    size = ButtonSize.Large,
                    onClick = onReportToDevelopers,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(Res.string.error_report))
                }

                VerticalSpacerD500()

                Text(stringResource(Res.string.error_report_aside))

                VerticalSpacerD1600()
            }
        }
    }
}

@Preview
@Composable
private fun BlockingErrorScreenPreview() {
    PreviewContent {
        BlockingErrorScreen(
            title = "Can't load apps",
            subtitle = "Double-check your connection and try again.",
            errorCode = 1001,
            onReportToDevelopers = {}
        )
    }
}
