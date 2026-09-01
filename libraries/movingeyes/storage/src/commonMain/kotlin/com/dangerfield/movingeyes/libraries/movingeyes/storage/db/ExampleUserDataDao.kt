package com.dangerfield.movingeyes.libraries.movingeyes.storage.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Placeholder table. Room refuses to generate a database with no entities,
 * and the app's real schema (scenes, eyes, groups) doesn't exist until the
 * scene-persistence phase. Delete this file, its `ProvideExampleUserDataDao`
 * binding, and its `AppDatabase` registration the moment the scene tables
 * land — nothing reads it.
 */
@Entity(tableName = "example_user_data")
data class ExampleUserDataEntity(
    @PrimaryKey val key: String,
    val value: String,
)

@Dao
interface ExampleUserDataDao {
    @Upsert
    suspend fun upsert(row: ExampleUserDataEntity)

    @Query("SELECT * FROM example_user_data WHERE key = :key")
    fun observe(key: String): Flow<ExampleUserDataEntity?>

    @Query("DELETE FROM example_user_data")
    suspend fun deleteAll()
}
