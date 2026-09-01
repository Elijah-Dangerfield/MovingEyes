package com.dangerfield.movingeyes.libraries.storage.impl.db

import com.dangerfield.movingeyes.libraries.scene.storage.db.SceneDao
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

/**
 * The pattern for exposing a DAO to the DI graph: delegate to the database
 * provider. Copy this for every DAO added to [AppDatabase].
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, boundType = SceneDao::class)
class ProvideSceneDao @Inject constructor(
    provider: AppDatabaseProvider
) : SceneDao by provider.database.sceneDao()
