package com.dangerfield.movingeyes.features.settings.impl.feedback

import com.dangerfield.movingeyes.libraries.core.eitherWay
import com.dangerfield.movingeyes.libraries.flowroutines.SEAViewModel
import com.dangerfield.movingeyes.libraries.movingeyes.AppCache
import com.dangerfield.movingeyes.libraries.navigation.Router
import com.dangerfield.movingeyes.libraries.ui.snackbar.showSnackBar
import me.tatarka.inject.annotations.Inject
import kotlin.time.Duration.Companion.seconds
import movingeyes.libraries.resources.generated.resources.Res
import movingeyes.libraries.resources.generated.resources.feedback_sent
import movingeyes.libraries.resources.generated.resources.feedback_empty
import org.jetbrains.compose.resources.getString

@Inject
class FeedbackViewModel(
    private val repository: FeedbackRepository,
    private val router: Router,
    private val appCache: AppCache,
) : SEAViewModel<FeedbackState, Unit, FeedbackAction>(
    initialStateArg = FeedbackState()
) {

    override suspend fun handleAction(action: FeedbackAction) {
        when (action) {
            FeedbackAction.Back -> router.goBack()
            is FeedbackAction.MessageChanged -> action.updateMessage()
            FeedbackAction.Submit -> action.submitFeedback()
        }
    }

    private suspend fun FeedbackAction.MessageChanged.updateMessage() {
        val updated = value
        updateState { it.copy(message = updated, errorMessage = null) }
    }

    private suspend fun FeedbackAction.submitFeedback() {
        val current = state
        if (current.message.isBlank()) {
            updateState { it.copy(errorMessage = getString(Res.string.feedback_empty)) }
            return
        }
        updateState { it.copy(isSubmitting = true, errorMessage = null) }
        repository.submitFeedback(
            message = current.message.trim(),
            isBugReport = false
        ).eitherWay {
            appCache.update { it.copy(feedbacksGiven = it.feedbacksGiven + 1) }
            updateState { it.copy(isSubmitting = false) }
            showSnackBar(message = getString(Res.string.feedback_sent), delayBy = 1.seconds)
            router.goBack()
        }
    }
}

data class FeedbackState(
    val message: String = "",
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface FeedbackAction {
    data object Back : FeedbackAction
    data class MessageChanged(val value: String) : FeedbackAction
    data object Submit : FeedbackAction
}
