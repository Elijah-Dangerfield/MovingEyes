package com.dangerfield.movingeyes.libraries.device.impl

import com.dangerfield.movingeyes.libraries.device.DisplayHost
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

/**
 * A no-op, because [AndroidDisplayController] already does both of these
 * itself with window flags. The binding exists only so the graph resolves the
 * same shape on both platforms.
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class AndroidDisplayHost : DisplayHost {
    override fun setChromeHidden(hidden: Boolean) = Unit
    override fun setOrientationLocked(locked: Boolean) = Unit
}
