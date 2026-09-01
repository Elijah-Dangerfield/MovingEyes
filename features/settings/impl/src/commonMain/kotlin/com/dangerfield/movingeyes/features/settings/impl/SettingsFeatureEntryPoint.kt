package com.dangerfield.movingeyes.features.settings.impl

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import com.dangerfield.movingeyes.features.paywall.PaywallRoute
import com.dangerfield.movingeyes.features.paywall.PaywallTrigger
import com.dangerfield.movingeyes.features.settings.SettingsRoute
import com.dangerfield.movingeyes.libraries.navigation.FeatureEntryPoint
import com.dangerfield.movingeyes.libraries.navigation.Router
import com.dangerfield.movingeyes.libraries.navigation.screen
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, multibinding = true)
@Inject
class SettingsFeatureEntryPoint(
    private val viewModelFactory: () -> SettingsViewModel,
) : FeatureEntryPoint {

    override fun NavGraphBuilder.buildNavGraph(router: Router) {
        screen<SettingsRoute> {
            val settingsViewModel = viewModel { viewModelFactory() }
            val state by settingsViewModel.stateFlow.collectAsStateWithLifecycle()

            SettingsScreen(
                isUnlocked = state.isUnlocked,
                reduceFlashing = state.reduceFlashing,
                muteAllSound = state.muteAllSound,
                versionName = state.versionName,
                onReduceFlashingChange = {
                    settingsViewModel.takeAction(SettingsAction.SetReduceFlashing(it))
                },
                onMuteChange = { settingsViewModel.takeAction(SettingsAction.SetMute(it)) },
                onUnlock = { router.navigate(PaywallRoute(PaywallTrigger.Settings)) },
                onRestore = { settingsViewModel.takeAction(SettingsAction.Restore) },
                onOpenPrivacy = { router.openWebLink(PrivacyUrl) },
                onOpenSupport = { router.openWebLink(SupportUrl) },
            )
        }
    }

    private companion object {
        /** Served from `pages/`; the same URLs go in both store listings. */
        const val PrivacyUrl = "https://elijahdangerfield.github.io/MovingEyes/privacy.html"

        /** The landing page carries the contact address. */
        const val SupportUrl = "https://elijahdangerfield.github.io/MovingEyes/"
    }
}
