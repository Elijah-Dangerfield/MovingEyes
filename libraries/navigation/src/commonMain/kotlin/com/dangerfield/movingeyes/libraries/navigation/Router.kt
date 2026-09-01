package com.dangerfield.movingeyes.libraries.navigation

import com.dangerfield.movingeyes.libraries.ui.snackbar.SnackBarPresenter
import com.dangerfield.movingeyes.libraries.core.Catching
import com.dangerfield.movingeyes.libraries.core.logging.KLog

data class NavigationOptions(
    val clearBackStack: Boolean = false,
    val launchSingleTop: Boolean = false,
    val restoreState: Boolean = false,
)

interface Router {

    fun navigate(route: Route, options: NavigationOptions = NavigationOptions())

    fun goBack()

    fun popBackTo(route: Route, inclusive: Boolean)

    fun openWebLink(url: String)
}

fun <T> Catching<T>.blockingScreenOnError(
    router: Router,
    title: String? = null,
    subtitle: String? = null,
    logId: String? = null,
    includeErrorMessage: Boolean = false,
): Catching<T> = this.onFailure {
    val errorCode = it.toKnownErrorCode()
    val resolvedLogId = logId ?: KLog.e(it)?.raw
    router.navigate(
        BlockingErrorRoute(
            title = title,
            subtitle = subtitle,
            errorCode = errorCode,
            logId = resolvedLogId,
            contextMessage = it.message.takeIf { includeErrorMessage }
        )
    )
}

fun <T> Catching<T>.dialogOnError(
    router: Router,
    title: String? = null,
    subtitle: String? = null,
    actionTitle: String? = null,
    action: ErrorDialogAction = ErrorDialogAction.Dismiss,
    logId: String? = null,
    includeErrorMessage: Boolean = false,
    ): Catching<T> = this.onFailure {
    val errorCode = it.toKnownErrorCode()
    val resolvedLogId = logId ?: KLog.e(it)?.raw
    router.navigate(
        ErrorDialogRoute(
            title = title,
            subtitle = subtitle,
            actionTitle = actionTitle,
            action = action,
            errorCode = errorCode,
            logId = resolvedLogId,
            contextMessage = it.message.takeIf { includeErrorMessage }
        )
    )
}

fun <T> Catching<T>.toastOnError(
    title: String = "Oops something went wrong",
    subtitle: String = "Please try again",
): Catching<T> = this.onFailure {
    SnackBarPresenter.show(
        title = title,
        message = subtitle,
    )
}