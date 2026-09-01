package com.dangerfield.movingeyes.libraries.scene.storage.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Eyes are deliberately not normalised into their own table. A scene is also
 * what gets exported as a shareable code, where no database exists, so
 * `SceneCodec` already versions its shape; normalising would mean expressing
 * every change twice and eventually having the two disagree.
 *
 * The broken-out columns are only what the drawer needs to sort and filter on
 * without decoding every row. The cost is that scenes can't be queried by
 * their contents, and nothing wants to.
 */
@Entity(tableName = "scenes")
data class SceneEntity(
    @PrimaryKey val id: String,

    val name: String,

    /** `SceneCodec.encode` output, versioned independently of this table. */
    val payload: String,

    val updatedAtMillis: Long,

    /** The working copy. Exactly one row has this set. */
    val isAutosave: Boolean,
)

@Dao
interface SceneDao {

    @Upsert
    suspend fun upsert(scene: SceneEntity)

    @Query("SELECT * FROM scenes WHERE isAutosave = 0 ORDER BY updatedAtMillis DESC")
    fun observeSaved(): Flow<List<SceneEntity>>

    @Query("SELECT * FROM scenes WHERE id = :id")
    suspend fun byId(id: String): SceneEntity?

    @Query("SELECT * FROM scenes WHERE isAutosave = 1 LIMIT 1")
    suspend fun autosave(): SceneEntity?

    @Query("DELETE FROM scenes WHERE id = :id")
    suspend fun delete(id: String)
}
