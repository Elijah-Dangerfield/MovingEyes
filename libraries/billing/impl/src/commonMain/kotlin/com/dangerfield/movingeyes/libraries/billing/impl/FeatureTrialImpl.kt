package com.dangerfield.movingeyes.libraries.billing.impl

import com.dangerfield.movingeyes.libraries.billing.ActiveTrial
import com.dangerfield.movingeyes.libraries.billing.DemoControl
import com.dangerfield.movingeyes.libraries.billing.FeatureTrial
import com.dangerfield.movingeyes.libraries.core.logging.KLog
import com.dangerfield.movingeyes.libraries.core.logging.logEvent
import com.dangerfield.movingeyes.libraries.flowroutines.AppCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn
import kotlin.time.Duration.Companion.milliseconds

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, boundType = FeatureTrial::class)
@Inject
class FeatureTrialImpl(
    appScope: AppCoroutineScope,
) : FeatureTrial by RealFeatureTrial(appScope)

/**
 * The demo controller. See [FeatureTrial] for the rules and why they're the
 * rules.
 *
 * Split from [FeatureTrialImpl] so the logic takes a plain [CoroutineScope] and
 * a test can drive thirty seconds of countdown through virtual time without
 * standing up the DI graph.
 */
class RealFeatureTrial(private val scope: CoroutineScope) : FeatureTrial {

    private val logger = KLog.withTag("FeatureTrial")

    private val _active = MutableStateFlow<ActiveTrial?>(null)
    override val active: StateFlow<ActiveTrial?> = _active.asStateFlow()

    private val _justEnded = MutableStateFlow<DemoControl?>(null)
    override val justEnded: StateFlow<DemoControl?> = _justEnded.asStateFlow()

    /**
     * Per *session*, not per install — this is deliberately in memory and
     * deliberately not persisted. Someone who comes back a week later to
     * decorate a different window gets to see the thing work again.
     */
    private val used = mutableSetOf<DemoControl>()

    private var countdown: Job? = null
    private var pendingRevert: (() -> Unit)? = null

    override fun isAvailable(control: DemoControl): Boolean =
        control !in used && _active.value == null

    override fun start(control: DemoControl, revert: () -> Unit): Boolean {
        if (!isAvailable(control)) return false

        used += control
        pendingRevert = revert
        _justEnded.value = null
        logger.logEvent("demo_started", "control" to control.name)

        countdown = scope.launch {
            var remaining = FeatureTrial.Duration
            _active.value = ActiveTrial(control, remaining)

            while (remaining > TickInterval) {
                delay(TickInterval)
                remaining -= TickInterval
                _active.value = ActiveTrial(control, remaining)
            }
            delay(remaining)

            expire(control)
        }
        return true
    }

    override fun keep() {
        // Drop the revert *before* cancelling, so the cancellation path can't
        // race it and take away something the user has now paid for.
        pendingRevert = null
        countdown?.cancel()
        countdown = null
        _active.value = null
        _justEnded.value = null
        logger.logEvent("demo_kept")
    }

    override fun cancel() {
        countdown?.cancel()
        countdown = null
        runRevert()
        _active.value = null
    }

    private fun expire(control: DemoControl) {
        runRevert()
        _active.value = null
        countdown = null
        _justEnded.value = control
        logger.logEvent("demo_expired", "control" to control.name)

        // The bar clears itself. It is not important enough to need dismissing,
        // and a banner that waits for a tap is a banner in the way of the
        // canvas the user is trying to look at.
        scope.launch {
            delay(FeatureTrial.EndedBannerDuration)
            if (_justEnded.value == control) _justEnded.value = null
        }
    }

    private fun runRevert() {
        val revert = pendingRevert ?: return
        pendingRevert = null
        revert()
    }

    private companion object {
        /**
         * Ten ticks a second. The countdown displays whole seconds, but the
         * last five are shown ticking, and a 1s cadence there looks stalled.
         */
        val TickInterval = 100.milliseconds
    }
}
