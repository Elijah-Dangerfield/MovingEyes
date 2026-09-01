package com.dangerfield.movingeyes.libraries.storage.impl.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import com.dangerfield.movingeyes.libraries.scene.storage.db.SceneDao
import com.dangerfield.movingeyes.libraries.scene.storage.db.SceneEntity

/**
 * The provider still falls back to destructive migration, which is data loss
 * once this has shipped. What keeps that survivable is [SceneEntity]'s shape:
 * a scene is one versioned payload column, so changing what a scene *is* goes
 * through `SceneCodec` and never touches this schema. If these columns ever do
 * need to change, write a real migration rather than bumping the version.
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
