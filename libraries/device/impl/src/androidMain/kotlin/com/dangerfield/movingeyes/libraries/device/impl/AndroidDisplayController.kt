package com.dangerfield.movingeyes.libraries.device.impl

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.WindowManager
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.dangerfield.movingeyes.libraries.device.DisplayController
import com.dangerfield.movingeyes.libraries.movingeyes.ActivityProvider
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

/**
 * Window flags on the foreground Activity.
 *
 * Every call is a no-op when there is no Activity — the app is backgrounded,
 * which is precisely when none of this matters. Silently doing nothing is
 * correct here and is why the interface promises best-effort.
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class AndroidDisplayController(
    private val activityProvider: ActivityProvider,
) : DisplayController {

    override fun setKeepAwake(keepAwake: Boolean) = onActivity { activity ->
        if (keepAwake) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    override fun setImmersive(immersive: Boolean) = onActivity { activity ->
        val controller = WindowInsetsControllerCompat(activity.window, activity.window.decorView)
        if (immersive) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
            // BY_SWIPE, not the default: the bars must come back when someone
            // swipes for them, because the swipe-from-edge back gesture is the
            // only documented way out of display mode. A sticky-immersive mode
            // that swallowed the first swipe would trap the user.
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    override fun setBrightness(brightness: Float?) = onActivity { activity ->
        activity.window.attributes = activity.window.attributes.apply {
            screenBrightness = brightness?.coerceIn(0f, 1f)
                ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        }
    }

    override fun setOrientationLocked(locked: Boolean) = onActivity { activity ->
        activity.requestedOrientation = if (locked) {
            // LOCKED rather than a specific orientation constant: it freezes
            // whatever the device is showing right now, which is the whole
            // point. Naming an orientation would spin a tablet that had been
            // taped up sideways.
            ActivityInfo.SCREEN_ORIENTATION_LOCKED
        } else {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    private inline fun onActivity(block: (Activity) -> Unit) {
        activityProvider.currentActivity()?.let(block)
    }
}
