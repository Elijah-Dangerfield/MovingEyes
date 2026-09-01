package com.dangerfield.movingeyes.features.editor.impl

import androidx.navigation.NavGraphBuilder
import com.dangerfield.movingeyes.features.editor.EditorRoute
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
class EditorFeatureEntryPoint : FeatureEntryPoint {

    override fun NavGraphBuilder.buildNavGraph(router: Router) {
        screen<EditorRoute> {
            EditorScreen()
        }
    }
}
