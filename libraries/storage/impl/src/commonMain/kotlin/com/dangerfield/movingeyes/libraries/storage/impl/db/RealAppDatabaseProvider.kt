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
     * `CoreTypeConverters` is not passed to the builder: Room rejects a
     * provided converter it doesn't need, and no entity currently has a column
     * that needs one. Add `.addTypeConverter(CoreTypeConverters())` back when
     * an entity takes an `Instant`, `List` or `Map` column.
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
