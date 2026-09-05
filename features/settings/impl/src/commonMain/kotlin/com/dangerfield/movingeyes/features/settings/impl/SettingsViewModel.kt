package com.dangerfield.movingeyes.features.settings.impl

import androidx.lifecycle.viewModelScope
import com.dangerfield.movingeyes.libraries.billing.Entitlements
import com.dangerfield.movingeyes.libraries.core.BuildInfo
import com.dangerfield.movingeyes.libraries.flowroutines.SEAViewModel
import com.dangerfield.movingeyes.libraries.movingeyes.AppCache
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import me.tatarka.inject.annotations.Inject

@Inject
class SettingsViewModel(
    private val appCache: AppCache,
    private val entitlements: Entitlements,
) : SEAViewModel<SettingsViewState, Nothing, SettingsAction>(
    initialStateArg = SettingsViewState(),
) {

    init {
        appCache.updates
            .onEach { takeAction(SettingsAction.Loaded(it.reduceFlashing)) }
            .launchIn(viewModelScope)

        entitlements.isUnlocked
            .onEach { takeAction(SettingsAction.UnlockChanged(it)) }
            .launchIn(viewModelScope)
    }

    override suspend fun handleAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.Loaded -> action.updateState {
                it.copy(reduceFlashing = action.reduceFlashing)
            }

            is SettingsAction.UnlockChanged -> action.updateState {
                it.copy(isUnlocked = action.isUnlocked)
            }

            is SettingsAction.SetReduceFlashing -> {
                appCache.update { it.copy(reduceFlashing = action.enabled) }
            }


            SettingsAction.Restore -> {
                entitlements.restore()
            }
        }
    }
}

data class SettingsViewState(
    val isUnlocked: Boolean = false,
    val reduceFlashing: Boolean = false,
    val versionName: String = BuildInfo.versionName,
)

sealed interface SettingsAction {
    data class Loaded(val reduceFlashing: Boolean) : SettingsAction
    data class UnlockChanged(val isUnlocked: Boolean) : SettingsAction
    data class SetReduceFlashing(val enabled: Boolean) : SettingsAction
    data object Restore : SettingsAction
}
