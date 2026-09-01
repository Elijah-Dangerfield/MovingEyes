package com.dangerfield.movingeyes.features.editor.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.dangerfield.movingeyes.libraries.device.BatteryNotice
import com.dangerfield.movingeyes.libraries.device.BatteryState
import com.dangerfield.movingeyes.libraries.device.BatteryStatus
import com.dangerfield.movingeyes.libraries.device.DisplayController
import com.dangerfield.movingeyes.libraries.device.dimLevelsFor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Display mode: the tablet is behind the painting and nobody is holding it.
 *
 * ## Why it is a state of the editor and not a screen
 *
 * v2 is explicit that the canvas must not move by a single pixel on entry. A
 * separate nav destination would remount the canvas and re-run layout, and any
 * reflow — even one that lands in the same place 99 times out of 100 — silently
 * invalidates an alignment the user spent four minutes on with a ruler. So only
 * the *chrome* animates. The eyes are the same composables, in the same places,
 * still running the same `EyeRuntime`s, from one frame to the next.
 */
class DisplayModeState(sleepTimer: Duration?) {

    var isActive by mutableStateOf(false)
        private set

    /** Fades the canvas out when the sleep timer expires. 1 = fully lit. */
    var sleepFade by mutableStateOf(1f)
        internal set

    /** The battery pill's current text, or null when nothing should show. */
    var batteryNotice by mutableStateOf<BatteryState?>(null)
        internal set

    var sleepTimer by mutableStateOf(sleepTimer)

    fun enter() {
        isActive = true
        sleepFade = 1f
    }

    fun exit() {
        isActive = false
        sleepFade = 1f
        batteryNotice = null
    }
}

/**
 * Drives the platform while [state] is active, and puts everything back when it
 * isn't.
 *
 * The restore is a `DisposableEffect` on purpose. Leaving keep-awake or a
 * brightness override set after the user has gone back to editing — or worse,
 * left the app — is the kind of bug that gets described in a review as "it
 * killed my battery", and it happens the first time an exit path is added that
 * forgets to clean up. Tying it to the composition means every exit path,
 * including the process being torn down mid-session, goes through the same
 * teardown.
 */
@Composable
fun DisplayModeEffects(
    state: DisplayModeState,
    brightness: Float,
    displayController: DisplayController,
    batteryStatus: BatteryStatus,
) {
    val active = state.isActive

    DisposableEffect(active) {
        displayController.setKeepAwake(active)
        displayController.setImmersive(active)
        // Orientation is frozen the instant display mode starts and not
        // before. While editing, rotation should follow the device; once the
        // tablet is taped to something, an accelerometer reading is noise and a
        // rotation is a ruined alignment.
        displayController.setOrientationLocked(active)

        onDispose {
            displayController.setKeepAwake(false)
            displayController.setImmersive(false)
            displayController.setOrientationLocked(false)
            displayController.setBrightness(null)
        }
    }

    // Split so a brightness drag doesn't re-run the whole entry sequence.
    DisposableEffect(active, brightness) {
        displayController.setBrightness(
            if (active) dimLevelsFor(brightness).osBrightness else null,
        )
        onDispose { }
    }

    BatteryNoticeEffect(state, batteryStatus)
    SleepTimerEffect(state)
}

/**
 * Shows the battery pill on entry as reassurance, then only when the charge is
 * genuinely low — and then once per ten-percent step rather than continuously.
 * See `BatteryNotice` for why the rule is shaped like that.
 */
@Composable
private fun BatteryNoticeEffect(state: DisplayModeState, batteryStatus: BatteryStatus) {
    // One per display session: it remembers which ten-percent step it has
    // already spoken about, so it has to outlive individual battery readings
    // but be forgotten when the session ends.
    val notice = remember(state.isActive) { BatteryNotice() }

    LaunchedEffect(state.isActive) {
        if (!state.isActive) return@LaunchedEffect

        // The reassurance glance. Three seconds is long enough to read a
        // percentage and short enough not to sit over the eyes.
        state.batteryNotice = batteryStatus.state.value
        delay(EntryGlance)
        state.batteryNotice = null

        batteryStatus.state.collectLatest { battery ->
            if (!notice.shouldAnnounce(battery)) return@collectLatest
            state.batteryNotice = battery
            delay(LowBatteryNotice)
            state.batteryNotice = null
        }
    }
}

/**
 * Fades to black over [SleepFadeDuration] rather than cutting.
 *
 * A hard cut at 11pm looks like the app crashed, and the person it wakes is
 * standing in a dark hallway wondering whether their tablet is dead. A fade
 * reads as the thing going to sleep, which is what it is.
 */
@Composable
private fun SleepTimerEffect(state: DisplayModeState) {
    val timer = state.sleepTimer

    LaunchedEffect(state.isActive, timer) {
        if (!state.isActive || timer == null) return@LaunchedEffect

        state.sleepFade = 1f
        delay(timer)

        val steps = SleepFadeSteps
        repeat(steps) { step ->
            state.sleepFade = 1f - (step + 1).toFloat() / steps
            delay(SleepFadeDuration / steps)
        }
    }
}

/** Timer options offered in the Scene panel. Null is "stay on all night". */
val SleepTimerOptions: List<Duration?> = listOf(null, 30.minutes, 60.minutes, 120.minutes, 240.minutes)

private val EntryGlance = 3.seconds
private val LowBatteryNotice = 6.seconds

/** Slow enough to read as going to sleep rather than as a fault. */
private val SleepFadeDuration = 8.seconds
private const val SleepFadeSteps = 80
