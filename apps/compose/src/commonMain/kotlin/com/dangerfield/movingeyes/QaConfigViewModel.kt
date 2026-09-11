package com.dangerfield.movingeyes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dangerfield.movingeyes.libraries.config.ConfigOverride
import com.dangerfield.movingeyes.libraries.config.ConfigOverrideRepository
import com.dangerfield.movingeyes.libraries.config.ConfiguredValue
import com.dangerfield.movingeyes.libraries.billing.Entitlements
import com.dangerfield.movingeyes.libraries.billing.PurchaseOutcome
import com.dangerfield.movingeyes.libraries.billing.RestoreOutcome
import com.dangerfield.movingeyes.libraries.config.QaConfigValue
import com.dangerfield.movingeyes.libraries.movingeyes.AppCache
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject

/**
 * Backs [QaConfigScreen]. Reads the whole `Set<QaConfigValue>` multibinding, so
 * every flag in the app is listed without anyone maintaining a list.
 *
 * Overrides are stored as raw strings. That's not laziness — `getValueRecursive`
 * coerces through `toString()` on the way out, so a typed value round-trips
 * correctly and the screen doesn't need to know whether a path holds an Int or
 * a Double.
 */
@Inject
class QaConfigViewModel(
    qaConfigValues: Set<QaConfigValue>,
    private val overrideRepository: ConfigOverrideRepository,
    private val entitlements: Entitlements,
    private val appCache: AppCache,
) : ViewModel() {

    val values: List<ConfiguredValue<*>> = qaConfigValues.filterIsInstance<ConfiguredValue<*>>()

    private val _overrides = MutableStateFlow(currentOverridePaths())
    val overrides: StateFlow<Map<String, Any>> = _overrides.asStateFlow()

    fun override(path: String, value: Any) {
        viewModelScope.launch {
            overrideRepository.addOverride(ConfigOverride(path, value))
            _overrides.value = currentOverridePaths()
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            overrideRepository.clearAll()
            _overrides.value = currentOverridePaths()
        }
    }

    val isUnlocked: StateFlow<Boolean> = entitlements.isUnlocked

    private val _lastBillingResult = MutableStateFlow<String?>(null)
    val lastBillingResult: StateFlow<String?> = _lastBillingResult.asStateFlow()

    fun purchase() {
        viewModelScope.launch {
            _lastBillingResult.value = when (val outcome = entitlements.purchase()) {
                PurchaseOutcome.Unlocked -> "Unlocked"
                PurchaseOutcome.Cancelled -> "Cancelled"
                PurchaseOutcome.StoreUnavailable -> "Store unavailable"
                is PurchaseOutcome.Failed -> "Failed: ${outcome.reason}"
            }
        }
    }

    fun restore() {
        viewModelScope.launch {
            _lastBillingResult.value = when (entitlements.restore()) {
                RestoreOutcome.Restored -> "Restored"
                RestoreOutcome.NothingToRestore -> "Nothing to restore"
                RestoreOutcome.StoreUnavailable -> "Store unavailable"
            }
        }
    }

    /**
     * The one thing that can revoke an unlock, and it exists only here. The
     * production path never clears a grant — see `Entitlements` — so without a
     * debug affordance there'd be no way to get back to the free tier and test
     * the paywall a second time.
     */
    fun clearEntitlement() {
        viewModelScope.launch {
            // ignoreStoreGrants at the same time, or this button does nothing
            // it claims to: the store still reports the account owns it, and
            // the next refresh grants it straight back.
            appCache.update {
                it.copy(isUnlocked = false, unlockedAtEpochMs = 0L, ignoreStoreGrants = true)
            }
            _lastBillingResult.value = "Cleared. The store can no longer re-grant it; buy to undo."
        }
    }

    private fun currentOverridePaths(): Map<String, Any> =
        overrideRepository.getOverrides().associate { it.path to it.value }
}
