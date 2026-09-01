package com.dangerfield.movingeyes.libraries.storage.impl.db

import com.dangerfield.movingeyes.libraries.movingeyes.storage.db.ExampleUserDataDao
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

/**
 * The pattern for exposing a DAO to the DI graph: delegate to the database
 * provider. Copy this for every DAO added to [AppDatabase].
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, boundType = ExampleUserDataDao::class)
class ProvideExampleUserDataDao @Inject constructor(
    provider: AppDatabaseProvider
) : ExampleUserDataDao by provider.database.exampleUserDataDao()
