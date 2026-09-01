package com.dangerfield.movingeyes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dangerfield.movingeyes.libraries.config.ConfigOverride
import com.dangerfield.movingeyes.libraries.config.ConfigOverrideRepository
import com.dangerfield.movingeyes.libraries.config.ConfiguredValue
import com.dangerfield.movingeyes.libraries.config.QaConfigValue
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

    private fun currentOverridePaths(): Map<String, Any> =
        overrideRepository.getOverrides().associate { it.path to it.value }
}
