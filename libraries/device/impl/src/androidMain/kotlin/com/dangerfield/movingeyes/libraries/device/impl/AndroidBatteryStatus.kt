package com.dangerfield.movingeyes.libraries.device.impl

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.dangerfield.movingeyes.libraries.device.BatteryState
import com.dangerfield.movingeyes.libraries.device.BatteryStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

/** The sticky `ACTION_BATTERY_CHANGED` broadcast, registered for the life of
 *  the process so the first value is available before display mode starts. */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class AndroidBatteryStatus(context: Context) : BatteryStatus {

    private val _state = MutableStateFlow(BatteryState())
    override val state: StateFlow<BatteryState> = _state.asStateFlow()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let { _state.value = it.toBatteryState() }
        }
    }

    init {
        // Returns the sticky intent, so the first value lands synchronously.
        context.applicationContext
            .registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?.let { _state.value = it.toBatteryState() }
    }

    private fun Intent.toBatteryState(): BatteryState {
        val level = getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val status = getIntExtra(BatteryManager.EXTRA_STATUS, -1)

        return BatteryState(
            // Scale is nearly always 100 but isn't guaranteed to be.
            percent = if (level >= 0 && scale > 0) level * 100 / scale else null,
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL,
        )
    }
}
