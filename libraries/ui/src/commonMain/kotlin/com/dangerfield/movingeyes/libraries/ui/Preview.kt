package com.dangerfield.movingeyes.libraries.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dangerfield.movingeyes.libraries.ui.components.AppBottomBar
import com.dangerfield.movingeyes.libraries.ui.components.BottomBarItem
import com.dangerfield.movingeyes.libraries.ui.components.dialog.DialogHost
import com.dangerfield.movingeyes.libraries.ui.components.dialog.LocalDialogHostState
import com.dangerfield.movingeyes.libraries.ui.components.dialog.rememberDialogHostState
import com.dangerfield.movingeyes.libraries.ui.system.LocalBuildInfo
import com.dangerfield.movingeyes.libraries.ui.system.LocalClock
import com.dangerfield.movingeyes.libraries.ui.system.color.ColorResource
import com.dangerfield.movingeyes.libraries.core.BuildInfo
import com.dangerfield.movingeyes.libraries.core.fixed
import com.dangerfield.movingeyes.system.AppThemeProvider
import com.dangerfield.movingeyes.system.background
import com.dangerfield.movingeyes.system.color.safelightColors
import com.dangerfield.movingeyes.system.thenIf
import kotlin.time.Clock
import kotlin.time.Instant

sealed class PreviewBottomBar(val render: @Composable () -> Unit) {

    object None: PreviewBottomBar({})

    object Home : PreviewBottomBar({
        AppBottomBar(
            items = listOf(
                BottomBarItem.Home(isSelected = true),
                BottomBarItem.Activity(isSelected = false),
                BottomBarItem.Profile(isSelected = false),
            ),
            onItemClick = {},
        )
    })

    object Activity : PreviewBottomBar({
        AppBottomBar(
            items = listOf(
                BottomBarItem.Home(isSelected = false),
                BottomBarItem.Activity(isSelected = true),
                BottomBarItem.Profile(isSelected = false),
            ),
            onItemClick = {},
        )
    })

    object Profile : PreviewBottomBar({
        AppBottomBar(
            items = listOf(
                BottomBarItem.Home(isSelected = false),
                BottomBarItem.Activity(isSelected = false),
                BottomBarItem.Profile(isSelected = true),
            ),
            onItemClick = {},
        )
    })
}

/**
 * A composable that is suitable as the root for any composable preview
 *
 * It will set up the theme and some suitable defaults like a background color.
 */
@Composable
fun PreviewContent(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    backgroundColor: ColorResource? = safelightColors.background,
    bottomBar: PreviewBottomBar = PreviewBottomBar.None,
    content: @Composable () -> Unit,
) {
    val dialogHostState = rememberDialogHostState()
    CompositionLocalProvider(
        LocalClock provides Clock.fixed(Instant.parse("2023-01-01T00:00:00Z")),
        LocalBuildInfo provides BuildInfo,
        LocalDialogHostState provides dialogHostState
    ) {
        AppThemeProvider {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .thenIf(backgroundColor != null) { background(backgroundColor!!) }
                    .padding(contentPadding),
            ) {
                content()
                Box(Modifier.align(Alignment.BottomCenter)) {
                    bottomBar.render()
                }

                DialogHost(
                    modifier = Modifier.matchParentSize(),
                    hostState = dialogHostState
                )
            }
        }
    }
}
