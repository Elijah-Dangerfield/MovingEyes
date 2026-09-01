package com.dangerfield.movingeyes.libraries.storage.impl.db

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.dangerfield.movingeyes.libraries.flowroutines.DispatcherProvider
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class RealAppDatabaseProvider @Inject constructor(
    private val builderFactory: AppDatabaseBuilderFactory,
    private val dispatcherProvider: DispatcherProvider
) : AppDatabaseProvider {

    /**
     * `CoreTypeConverters` is deliberately **not** passed to the builder.
     *
     * Room rejects a provided converter it doesn't need — "Unexpected type
     * converter … remove this converter from the builder" — and no entity
     * currently has a column that needs one. `SceneEntity` stores its timestamp
     * as a Long precisely so it doesn't.
     *
     * Add `.addTypeConverter(CoreTypeConverters())` back at the moment an
     * entity takes an `Instant`, a `List<String>` or a `Map` column, and not
     * before. This crashed on first launch for exactly this reason once the
     * database started actually being opened.
     */
    override val database: AppDatabase by lazy {
        builderFactory
            .create()
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(dispatcherProvider.io)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }
}
