package com.dangerfield.movingeyes.libraries.navigation.floatingwindow

import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import com.dangerfield.movingeyes.libraries.ui.components.dialog.DialogState
import com.dangerfield.movingeyes.libraries.ui.components.dialog.rememberDestinationDialogState

@Composable
fun DialogDestination(
    backStackEntry: NavBackStackEntry,
    content: @Composable (NavBackStackEntry, DialogState) -> Unit
) {
    val dialogState = rememberDestinationDialogState(backStackEntry.id)
    content(backStackEntry, dialogState)
}
