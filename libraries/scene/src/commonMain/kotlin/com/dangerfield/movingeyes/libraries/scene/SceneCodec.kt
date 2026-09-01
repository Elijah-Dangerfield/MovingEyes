package com.dangerfield.movingeyes.libraries.scene

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * A scene as stored, in a `{"v": <int>, "d": {...}}` envelope matching
 * `VersionedCacheJsonSerializer`.
 *
 * Versioned from the first commit that has it, not the first release: a scene
 * is the one artifact this app exists to produce, and payloads already written
 * unlabelled can't be migrated later.
 *
 * To add a version, append a [Migration] rewriting the previous version's JSON
 * into the next. [CurrentVersion] follows the list length so the two can't
 * drift. Never edit a migration that has shipped. Adding an optional field
 * needs no migration — unknown keys are ignored on read.
 */
object SceneCodec {

    private val json = Json {
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
     * Written by a newer build, most likely restored from a backup. Refused
     * rather than half-read: showing a partly-understood composition invites
     * the user to re-save it over the good copy. Distinct from [Unreadable]
     * because the scene is probably fine.
     */
    data class FromTheFuture(val version: Int, val supported: Int) : SceneDecodeResult

    data class Unreadable(val reason: String) : SceneDecodeResult
}
