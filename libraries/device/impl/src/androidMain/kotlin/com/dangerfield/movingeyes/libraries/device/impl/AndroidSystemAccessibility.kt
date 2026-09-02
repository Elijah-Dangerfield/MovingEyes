package com.dangerfield.movingeyes.libraries.device.impl

import android.content.Context
import android.provider.Settings
import com.dangerfield.movingeyes.libraries.device.SystemAccessibility
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

/**
 * Android has no "reduce motion" flag; the accepted proxy is the animator
 * duration scale being zeroed, which is what the accessibility setting and
 * developer options both do.
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class AndroidSystemAccessibility(private val context: Context) : SystemAccessibility {

    override val isReduceMotionEnabled: Boolean
        get() = Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
}
