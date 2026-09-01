package com.dangerfield.movingeyes.libraries.storage.impl.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import com.dangerfield.movingeyes.libraries.scene.storage.db.SceneDao
import com.dangerfield.movingeyes.libraries.scene.storage.db.SceneEntity

/**
 * ## A note on migrations, now that this database holds real work
 *
 * The provider is still on `fallbackToDestructiveMigration`, which is fine
 * while nothing has shipped and would be data loss the moment it has: `scenes`
 * is where twenty minutes of lining eyes up with holes in cardboard ends up.
 *
 * What keeps that from being a live grenade is the shape of the table. A scene
 * is stored as one versioned payload column, so changing what a scene *is*
 * goes through `SceneCodec` and never touches this schema — see [SceneEntity].
 * The columns here exist only to sort and filter, and they aren't expected to
 * change again.
 *
 * If they ever do, write a real migration before release rather than bumping
 * the version.
 */
@Database(
    entities = [
        SceneEntity::class,
    ],
    version = 6, // Bumped: the example placeholder table replaced by scenes
    exportSchema = true
)
@TypeConverters(CoreTypeConverters::class)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sceneDao(): SceneDao
}

@Suppress("KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}
