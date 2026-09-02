package com.dangerfield.movingeyes.libraries.device.impl

import com.dangerfield.movingeyes.libraries.device.SystemAccessibility
import me.tatarka.inject.annotations.Inject
import platform.UIKit.UIAccessibilityIsReduceMotionEnabled
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class IosSystemAccessibility : SystemAccessibility {

    override val isReduceMotionEnabled: Boolean
        get() = UIAccessibilityIsReduceMotionEnabled()
}
