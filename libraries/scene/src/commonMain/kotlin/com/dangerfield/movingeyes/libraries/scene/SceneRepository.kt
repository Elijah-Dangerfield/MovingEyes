package com.dangerfield.movingeyes.libraries.scene

import kotlinx.coroutines.flow.Flow

/**
 * One autosave slot plus however many scenes the user has named.
 *
 * Autosave is the *working copy*, not a backup: the editor is always editing
 * something, and the app opens on a live canvas, so that something has to
 * survive a kill without anybody pressing Save. Saving is the separate act of
 * naming an arrangement so you can come back to it.
 */
interface SceneRepository {

    fun observeSaved(): Flow<List<Scene>>

    suspend fun loadAutosave(): Scene?

    /** Called on every gesture end, so it must stay cheap. */
    suspend fun writeAutosave(scene: Scene)

    suspend fun save(scene: Scene)

    suspend fun load(id: String): Scene?

    suspend fun delete(id: String)
}
