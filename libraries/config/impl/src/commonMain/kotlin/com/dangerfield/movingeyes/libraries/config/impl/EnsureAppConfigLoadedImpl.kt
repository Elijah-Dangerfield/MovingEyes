package com.dangerfield.movingeyes.libraries.config.impl

import com.dangerfield.movingeyes.libraries.config.AppConfigRepository
import com.dangerfield.movingeyes.libraries.config.EnsureAppConfigLoaded
import com.dangerfield.movingeyes.libraries.core.Catching
import com.dangerfield.movingeyes.libraries.core.ignoreValue
import com.dangerfield.movingeyes.libraries.core.throwIfDebug
import kotlinx.coroutines.flow.first
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class EnsureAppConfigLoadedImpl @Inject constructor(
    private val repository: AppConfigRepository
) : EnsureAppConfigLoaded {
    override suspend fun invoke(): Catching<Unit> =
        Catching {
            repository.configStream().first()
        }
            .throwIfDebug()
            .ignoreValue()
}
