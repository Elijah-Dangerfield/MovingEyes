package com.dangerfield.movingeyes.features.editor.impl

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import com.dangerfield.movingeyes.features.editor.EditorRoute
import com.dangerfield.movingeyes.libraries.device.ScreenMetrics
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
class EditorFeatureEntryPoint(
    private val screenMetrics: ScreenMetrics,
    private val viewModelFactory: () -> EditorViewModel,
) : FeatureEntryPoint {

    override fun NavGraphBuilder.buildNavGraph(router: Router) {
        screen<EditorRoute> {
            // `viewModel { }` and not a bare call to the factory: the block is a
            // composable that re-runs, and constructing the view model directly
            // would build a new one on every recomposition — losing the loaded
            // scene and restarting the autosave lookup each time.
            EditorScreen(
                viewModel = viewModel { viewModelFactory() },
                screenMetrics = screenMetrics,
            )
        }
    }
}
