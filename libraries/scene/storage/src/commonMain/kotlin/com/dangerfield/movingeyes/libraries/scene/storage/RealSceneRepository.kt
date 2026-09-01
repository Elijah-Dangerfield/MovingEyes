package com.dangerfield.movingeyes.libraries.scene.storage

import com.dangerfield.movingeyes.libraries.core.logging.KLog
import com.dangerfield.movingeyes.libraries.scene.Scene
import com.dangerfield.movingeyes.libraries.scene.SceneCodec
import com.dangerfield.movingeyes.libraries.scene.SceneDecodeResult
import com.dangerfield.movingeyes.libraries.scene.SceneRepository
import com.dangerfield.movingeyes.libraries.scene.storage.db.SceneDao
import com.dangerfield.movingeyes.libraries.scene.storage.db.SceneEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn
import kotlin.time.Clock

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, boundType = SceneRepository::class)
@Inject
class RealSceneRepository(
    private val dao: SceneDao,
    private val clock: Clock,
) : SceneRepository {

    private val logger = KLog.withTag("Scenes")

    /** A row that won't decode is skipped: one bad scene must not take the
     *  whole drawer down with it. */
    override fun observeSaved(): Flow<List<Scene>> =
        dao.observeSaved().map { rows -> rows.mapNotNull { it.toSceneOrNull() } }

    override suspend fun loadAutosave(): Scene? = dao.autosave()?.toSceneOrNull()

    override suspend fun writeAutosave(scene: Scene) {
        dao.upsert(scene.toEntity(id = AutosaveId, isAutosave = true))
    }

    override suspend fun save(scene: Scene) {
        dao.upsert(scene.toEntity(id = scene.id, isAutosave = false))
    }

    override suspend fun load(id: String): Scene? = dao.byId(id)?.toSceneOrNull()

    override suspend fun delete(id: String) = dao.delete(id)

    private fun Scene.toEntity(id: String, isAutosave: Boolean) = SceneEntity(
        id = id,
        name = name,
        payload = SceneCodec.encode(this.copy(id = id)),
        updatedAtMillis = clock.now().toEpochMilliseconds(),
        isAutosave = isAutosave,
    )

    private fun SceneEntity.toSceneOrNull(): Scene? =
        when (val result = SceneCodec.decode(payload)) {
            is SceneDecodeResult.Success -> result.scene

            is SceneDecodeResult.FromTheFuture -> {
                logger.e {
                    "Scene $id was written by payload v${result.version}, this build reads " +
                        "up to v${result.supported}. Skipping rather than showing a wrong scene."
                }
                null
            }

            is SceneDecodeResult.Unreadable -> {
                logger.e { "Scene $id is unreadable: ${result.reason}" }
                null
            }
        }

    private companion object {
        const val AutosaveId = "autosave"
    }
}
