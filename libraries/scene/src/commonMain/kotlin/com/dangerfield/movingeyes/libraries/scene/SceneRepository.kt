package com.dangerfield.movingeyes.libraries.scene

import kotlinx.coroutines.flow.Flow

/**
 * Where scenes live. One autosave slot plus however many the user has named.
 *
 * ## The autosave slot
 *
 * The editor is always editing *something*, and that something survives a kill
 * without anybody pressing Save. v2 cut onboarding on the grounds that the app
 * should open on a live canvas, and that only holds if the canvas it opens on
 * is the one you left. So the current composition is continuously written to a
 * single reserved row, and "Save" is the separate act of giving an arrangement
 * a name so you can come back to it after building something else.
 *
 * Autosave is therefore not a backup of a saved scene. It's the working copy.
 */
interface SceneRepository {

    /** Named scenes, most recently touched first. */
    fun observeSaved(): Flow<List<Scene>>

    /** The working copy, or null on a genuinely first launch. */
    suspend fun loadAutosave(): Scene?

    /**
     * Overwrite the working copy. Called often — on every gesture end — so it
     * must stay cheap and must never block a frame.
     */
    suspend fun writeAutosave(scene: Scene)

    /** Give the current arrangement a name and keep it. */
    suspend fun save(scene: Scene)

    suspend fun load(id: String): Scene?

    suspend fun delete(id: String)
}
