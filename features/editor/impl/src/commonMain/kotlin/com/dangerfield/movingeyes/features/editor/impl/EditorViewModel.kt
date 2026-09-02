package com.dangerfield.movingeyes.features.editor.impl

import com.dangerfield.movingeyes.libraries.billing.Entitlements
import com.dangerfield.movingeyes.libraries.billing.FeatureTrial
import com.dangerfield.movingeyes.libraries.core.logging.KLog
import com.dangerfield.movingeyes.libraries.device.BatteryStatus
import com.dangerfield.movingeyes.libraries.device.DisplayController
import com.dangerfield.movingeyes.libraries.device.SystemAccessibility
import com.dangerfield.movingeyes.libraries.eyes.Mood
import com.dangerfield.movingeyes.libraries.core.logging.logEvent
import com.dangerfield.movingeyes.libraries.movingeyes.AppCache
import com.dangerfield.movingeyes.libraries.movingeyes.Permission
import com.dangerfield.movingeyes.libraries.movingeyes.PermissionManager
import com.dangerfield.movingeyes.libraries.movingeyes.PermissionResult
import com.dangerfield.movingeyes.libraries.movingeyes.PermissionStatus
import com.dangerfield.movingeyes.libraries.reactivity.ReactivitySource
import com.dangerfield.movingeyes.libraries.review.ReviewPromptCoordinator
import com.dangerfield.movingeyes.libraries.review.ReviewTrigger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import androidx.lifecycle.viewModelScope
import com.dangerfield.movingeyes.libraries.flowroutines.SEAViewModel
import com.dangerfield.movingeyes.libraries.scene.Scene
import com.dangerfield.movingeyes.libraries.scene.SceneRepository
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import me.tatarka.inject.annotations.Inject
import kotlin.time.Clock

/**
 * Owns everything about the editor that outlives a composition: which scenes
 * exist, which one is open, and whether the user has paid.
 *
 * The *live* editing state deliberately does not live here — it's in
 * [EditorState], next to the renderer, because a drag mutates it sixty times a
 * second and routing that through a ViewModel's action channel would put a
 * coroutine hop between a finger and a pixel.
 */
@Inject
class EditorViewModel(
    private val sceneRepository: SceneRepository,
    private val clock: Clock,
    private val appCache: AppCache,
    val entitlements: Entitlements,
    val featureTrial: FeatureTrial,
    private val reviewPrompts: ReviewPromptCoordinator,
    private val permissions: PermissionManager,
    private val accessibility: SystemAccessibility,
    val reactivity: ReactivitySource,
    val displayController: DisplayController,
    val batteryStatus: BatteryStatus,
) : SEAViewModel<EditorViewState, EditorEvent, EditorAction>(
    initialStateArg = EditorViewState(),
) {

    private val logger = KLog.withTag("Editor")

    /** True when the OS will show its own prompt, so our explanation goes first. */
    fun needsMicrophoneExplanation(): Boolean =
        permissions.checkPermissionStatus(Permission.Microphone) != PermissionStatus.GRANTED

    init {
        // Routed through an action rather than updated directly: state on a
        // SEAViewModel only ever moves via an action, so the saved-scene list
        // is no exception even though nobody taps to cause it.
        sceneRepository.observeSaved()
            .onEach { scenes -> takeAction(EditorAction.ScenesChanged(scenes)) }
            .launchIn(viewModelScope)

        appCache.updates
            .onEach { data ->
                takeAction(
                    EditorAction.SettingsChanged(
                        seenHint = data.hasSeenDisplayModeHint,
                        // The OS preference counts as the setting being on:
                        // someone who set it system-wide should not have to
                        // find it again in here.
                        reduceFlashing = data.reduceFlashing || accessibility.isReduceMotionEnabled,
                        moodsWarnedAbout = data.flashingWarningsSeen,
                    ),
                )
            }
            .launchIn(viewModelScope)

        takeAction(EditorAction.LoadAutosave)
    }

    override suspend fun handleAction(action: EditorAction) {
        when (action) {
            is EditorAction.ScenesChanged -> {
                action.updateState { it.copy(savedScenes = action.scenes) }
            }

            EditorAction.LoadAutosave -> {
                val restored = sceneRepository.loadAutosave()
                action.updateState { it.copy(openScene = restored, isLoaded = true) }
            }

            is EditorAction.Open -> {
                action.updateState { it.copy(openScene = action.scene) }
                // Opened scenes are autosaved immediately rather than waiting
                // for the first gesture. Without this, picking a scene and then
                // losing the app to a low-memory kill reopens the *previous*
                // one, which reads as the app having forgotten what you chose.
                sceneRepository.writeAutosave(action.scene)
                sendEvent(EditorEvent.SceneOpened(action.scene))
            }

            /**
             * Autosave is the working copy, not a backup — see [SceneRepository].
             * It's written on gesture end rather than on a timer, so the thing
             * on disk is always a composition the user finished making rather
             * than one caught mid-drag.
             */
            is EditorAction.Autosave -> {
                sceneRepository.writeAutosave(action.scene)
            }

            is EditorAction.Save -> {
                val named = action.scene.copy(id = clock.now().toEpochMilliseconds().toString())
                sceneRepository.save(named)
                logger.d { "Saved scene ${named.id}" }
                sendEvent(EditorEvent.SceneSaved)
            }

            is EditorAction.SettingsChanged -> {
                action.updateState {
                    it.copy(
                        hasSeenDisplayModeHint = action.seenHint,
                        reduceFlashing = action.reduceFlashing,
                        moodsWarnedAbout = action.moodsWarnedAbout,
                    )
                }
            }

            is EditorAction.DisplaySessionEnded -> {
                logger.logEvent(
                    "display_session_ended",
                    "durationSeconds" to action.duration.inWholeSeconds.toString(),
                )
                // Only after a session long enough to mean the thing actually
                // worked — a thirty-second look is not a positive moment.
                if (action.duration >= ReviewWorthySession) {
                    reviewPrompts.requestPrompt(ReviewTrigger.MilestoneReached)
                }
            }

            EditorAction.RequestMicrophone -> {
                val granted = permissions.ensurePermission(Permission.Microphone)
                sendEvent(
                    if (granted is PermissionResult.Granted) {
                        EditorEvent.MicrophoneGranted
                    } else {
                        EditorEvent.MicrophoneUnavailable
                    },
                )
            }

            EditorAction.StartReactivity -> {
                if (!reactivity.start()) sendEvent(EditorEvent.MicrophoneUnavailable)
            }

            EditorAction.StopReactivity -> reactivity.stop()

            EditorAction.OpenAppSettings -> permissions.openAppSettings()

            EditorAction.ReduceFlashing -> {
                appCache.update { it.copy(reduceFlashing = true) }
            }

            is EditorAction.FlashingWarningSeen -> {
                appCache.update {
                    it.copy(flashingWarningsSeen = it.flashingWarningsSeen + action.mood.name)
                }
            }

            EditorAction.DisplayHintSeen -> {
                appCache.update { it.copy(hasSeenDisplayModeHint = true) }
            }

            is EditorAction.Delete -> sceneRepository.delete(action.scene.id)
        }
    }
}

data class EditorViewState(
    val savedScenes: List<Scene> = emptyList(),

    /** The scene to build the canvas from. Null until [isLoaded]. */
    val openScene: Scene? = null,

    /**
     * False until the autosave lookup has answered. The canvas waits for this
     * rather than starting blank, because building a default scene and then
     * replacing it would flash two eyes the user didn't place.
     */
    val isLoaded: Boolean = false,

    /** The one-time "taps do nothing now" card has been dismissed. */
    val hasSeenDisplayModeHint: Boolean = false,

    val reduceFlashing: Boolean = false,

    /** Mood names whose photosensitivity warning has already been shown. */
    val moodsWarnedAbout: Set<String> = emptySet(),
)

private val ReviewWorthySession = 10.minutes

sealed interface EditorEvent {
    data class SceneOpened(val scene: Scene) : EditorEvent
    data object SceneSaved : EditorEvent
    data object MicrophoneGranted : EditorEvent
    data object MicrophoneUnavailable : EditorEvent
}

sealed interface EditorAction {
    data object LoadAutosave : EditorAction
    data class ScenesChanged(val scenes: List<Scene>) : EditorAction
    data class Open(val scene: Scene) : EditorAction
    data class Autosave(val scene: Scene) : EditorAction
    data class Save(val scene: Scene) : EditorAction
    data class Delete(val scene: Scene) : EditorAction
    data class SettingsChanged(
        val seenHint: Boolean,
        val reduceFlashing: Boolean,
        val moodsWarnedAbout: Set<String>,
    ) : EditorAction

    data class FlashingWarningSeen(val mood: Mood) : EditorAction
    data object ReduceFlashing : EditorAction
    data class DisplaySessionEnded(val duration: Duration) : EditorAction
    data object RequestMicrophone : EditorAction
    data object StartReactivity : EditorAction
    data object StopReactivity : EditorAction
    data object OpenAppSettings : EditorAction
    data object DisplayHintSeen : EditorAction
}
