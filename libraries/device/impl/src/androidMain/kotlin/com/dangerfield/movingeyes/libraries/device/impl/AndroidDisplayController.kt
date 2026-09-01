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
 * Window flags on the foreground Activity. A no-op when there isn't one, which
 * is when the app is backgrounded and none of this matters.
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
            // BY_SWIPE, not sticky: the swipe-from-edge back gesture is the
            // only way out of display mode, so it must not be swallowed.
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
            // LOCKED freezes what's on screen; naming an orientation would spin
            // a tablet that had been taped up sideways.
            ActivityInfo.SCREEN_ORIENTATION_LOCKED
        } else {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    private inline fun onActivity(block: (Activity) -> Unit) {
        activityProvider.currentActivity()?.let(block)
    }
}
