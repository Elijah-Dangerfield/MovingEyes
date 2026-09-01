package com.dangerfield.movingeyes.libraries.device.impl

import com.dangerfield.movingeyes.libraries.device.BatteryState
import com.dangerfield.movingeyes.libraries.device.BatteryStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.tatarka.inject.annotations.Inject
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIDevice
import platform.UIKit.UIDeviceBatteryLevelDidChangeNotification
import platform.UIKit.UIDeviceBatteryState
import platform.UIKit.UIDeviceBatteryStateDidChangeNotification
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn
import kotlin.math.roundToInt

/** `UIDevice` battery reporting is off by default and returns -1 until it's
 *  switched on. */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class IosBatteryStatus : BatteryStatus {

    private val _state = MutableStateFlow(BatteryState())
    override val state: StateFlow<BatteryState> = _state.asStateFlow()

    init {
        UIDevice.currentDevice.batteryMonitoringEnabled = true
        observe(UIDeviceBatteryLevelDidChangeNotification)
        observe(UIDeviceBatteryStateDidChangeNotification)
        refresh()
    }

    private fun observe(name: String?) {
        NSNotificationCenter.defaultCenter.addObserverForName(
            name = name,
            `object` = null,
            queue = NSOperationQueue.mainQueue,
        ) { _ -> refresh() }
    }

    private fun refresh() {
        val device = UIDevice.currentDevice
        val level = device.batteryLevel
        _state.value = BatteryState(
            // -1 is "not known yet", not empty.
            percent = if (level < 0f) null else (level * 100f).roundToInt(),
            isCharging = device.batteryState == UIDeviceBatteryState.UIDeviceBatteryStateCharging ||
                device.batteryState == UIDeviceBatteryState.UIDeviceBatteryStateFull,
        )
    }
}
