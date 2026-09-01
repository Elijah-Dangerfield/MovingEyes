package com.dangerfield.movingeyes.libraries.scene.storage.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * A scene row.
 *
 * ## Why the whole scene is one opaque column
 *
 * Eyes are *not* normalised into their own table, and that's deliberate.
 * `SceneCodec` already has to version the scene's shape, because a scene is
 * also what gets exported as a shareable code and imported on another device
 * where no database is involved. Normalising would mean the same shape change
 * had to be expressed twice — once as a codec migration and once as a Room
 * migration — and the two would eventually disagree.
 *
 * With a payload column, the table's shape is fixed: adding a field to a scene
 * touches the codec and nothing here. The columns that *are* broken out are
 * only the ones the database needs to sort and filter on without decoding
 * every row.
 *
 * The cost is that scenes can't be queried by their contents. Nothing wants to.
 */
@Entity(tableName = "scenes")
data class SceneEntity(
    @PrimaryKey val id: String,

    /** Duplicated out of the payload so the drawer can list names without
     *  decoding every scene it might show. */
    val name: String,

    /** `SceneCodec.encode` output. Versioned independently of this table. */
    val payload: String,

    val updatedAtMillis: Long,

    /** The working copy. Exactly one row has this set — see [SceneRepository]. */
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
