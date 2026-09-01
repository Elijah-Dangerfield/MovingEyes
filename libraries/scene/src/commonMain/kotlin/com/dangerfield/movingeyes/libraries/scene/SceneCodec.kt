package com.dangerfield.movingeyes.libraries.scene

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * Turns a [Scene] into the string that gets stored, and back.
 *
 * ## Why this is versioned on day one
 *
 * A scene is the only thing in this app the user actually *makes*. Someone
 * spends twenty minutes lining eyes up with holes they cut in cardboard; if a
 * later release can't read that back, the app has destroyed the only artifact
 * it exists to produce. Adding versioning after the first release is too late,
 * because v1's payloads are already on disk unlabelled.
 *
 * The envelope is `{"v": <int>, "d": {...}}`, deliberately the same shape as
 * `VersionedCacheJsonSerializer` uses, so there is one thing to learn.
 *
 * ## Adding a version
 *
 * Append a [Migration] to [migrations] that rewrites the previous version's
 * JSON into the next one's. [CurrentVersion] is derived from the list length,
 * so it can't drift out of step with the migrations that are actually present.
 * Never edit an existing migration — it runs against payloads already written.
 *
 * Unknown keys are ignored on read, so a *newer* build adding an optional field
 * doesn't need a migration at all; a migration is for renames, removals and
 * changes of meaning.
 */
object SceneCodec {

    private val json = Json {
        // A field added in a later version must not make an older reader throw;
        // it should read what it understands and drop the rest.
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    /** Rewrites version N's payload into version N+1's. Index 0 is 1 → 2. */
    fun interface Migration {
        fun migrate(data: JsonElement): JsonElement
    }

    private val migrations: List<Migration> = emptyList()

    val CurrentVersion: Int = migrations.size + 1

    fun encode(scene: Scene): String {
        val envelope = buildJsonObject {
            put("v", JsonPrimitive(CurrentVersion))
            put("d", json.encodeToJsonElement(Scene.serializer(), scene))
        }
        return json.encodeToString(JsonObject.serializer(), envelope)
    }

    fun decode(payload: String): SceneDecodeResult {
        val root = runCatching { json.parseToJsonElement(payload) }.getOrNull() as? JsonObject
            ?: return SceneDecodeResult.Unreadable("payload is not a JSON object")

        val version = root["v"]?.jsonPrimitive?.intOrNull
            ?: return SceneDecodeResult.Unreadable("missing version")

        // A scene written by a newer build than this one. Refusing is the only
        // honest answer: guessing would show the user a composition that isn't
        // the one they saved, and re-saving it would overwrite the good copy
        // with a lossy one.
        if (version > CurrentVersion) {
            return SceneDecodeResult.FromTheFuture(version = version, supported = CurrentVersion)
        }

        val data = root["d"] ?: return SceneDecodeResult.Unreadable("missing payload")

        val migrated = migrations
            .drop(version - 1)
            .fold(data) { element, migration ->
                runCatching { migration.migrate(element) }.getOrElse {
                    return SceneDecodeResult.Unreadable("migration from v$version failed: ${it.message}")
                }
            }

        return runCatching { json.decodeFromJsonElement(Scene.serializer(), migrated) }
            .fold(
                onSuccess = { SceneDecodeResult.Success(it) },
                onFailure = { SceneDecodeResult.Unreadable(it.message ?: "malformed scene") },
            )
    }
}

sealed interface SceneDecodeResult {
    data class Success(val scene: Scene) : SceneDecodeResult

    /**
     * Written by a newer version of the app — most likely restored from a
     * backup onto an older build. Distinct from [Unreadable] because the scene
     * is probably fine and the user should be told to update rather than told
     * their work is corrupt.
     */
    data class FromTheFuture(val version: Int, val supported: Int) : SceneDecodeResult

    data class Unreadable(val reason: String) : SceneDecodeResult
}
