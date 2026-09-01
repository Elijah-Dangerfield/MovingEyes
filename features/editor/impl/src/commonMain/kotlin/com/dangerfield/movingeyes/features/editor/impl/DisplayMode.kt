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
 * A state of the editor, not a screen: a separate destination would remount
 * the canvas and re-run layout, and any reflow silently invalidates an
 * alignment someone measured with a ruler. Only the chrome animates.
 */
class DisplayModeState(sleepTimer: Duration?) {

    var isActive by mutableStateOf(false)
        private set

    /** 1 = fully lit. */
    var sleepFade by mutableStateOf(1f)
        internal set

    /** Null when the pill should be hidden. */
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
 * Drives the platform while [state] is active and puts everything back when it
 * isn't. The restore is a `DisposableEffect` so every exit path — including a
 * teardown mid-session — releases keep-awake and the brightness override.
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

/** Once on entry as reassurance, then only when low. See `BatteryNotice`. */
@Composable
private fun BatteryNoticeEffect(state: DisplayModeState, batteryStatus: BatteryStatus) {
    // One per session: it remembers which band it has already spoken about.
    val notice = remember(state.isActive) { BatteryNotice() }

    LaunchedEffect(state.isActive) {
        if (!state.isActive) return@LaunchedEffect

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

/** Fades rather than cuts: a hard cut looks like the app crashed. */
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

/** Null is "stay on all night". */
val SleepTimerOptions: List<Duration?> = listOf(null, 30.minutes, 60.minutes, 120.minutes, 240.minutes)

private val EntryGlance = 3.seconds
private val LowBatteryNotice = 6.seconds
private val SleepFadeDuration = 8.seconds
private const val SleepFadeSteps = 80
