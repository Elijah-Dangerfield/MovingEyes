package com.dangerfield.movingeyes.libraries.movingeyes

import com.dangerfield.movingeyes.libraries.storage.Cache
import com.dangerfield.movingeyes.libraries.storage.CacheFactory
import com.dangerfield.movingeyes.libraries.storage.versionedJsonSerializer
import kotlinx.serialization.Serializable
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

/**
 * In-memory + persistent cache for app-wide state that doesn't need to be in the database.
 */
@Serializable
data class AppData(
    /**
     * Stable per-install identifier, minted on first read and persisted for
     * the app's lifetime on this device (survives sign-out; dies with
     * uninstall). Sent as X-Install-Id on authenticated requests so the
     * server can associate anonymous accounts from the same install.
     * Stored as a string (UUID canonical form) so the JSON serializer
     * doesn't need a Uuid-aware adapter on every cache read.
     */
    val installId: String? = null,

    // Screen visits - automatically tracked for any TrackableRoute
    val screenVisits: Map<String, Int> = emptyMap(),

    /**
     * The unlock, cached locally.
     *
     * Deliberately sticky: once true it is only ever cleared from the QA menu,
     * never by the store answering "not owned". A store hiccup, a signed-out
     * account, or a tablet with no wifi must not send a paying customer's
     * mounted decoration back to the free tier at 8pm on Halloween. See
     * `Entitlements` for the full argument and what it costs.
     */
    val isUnlocked: Boolean = false,

    /** Epoch-ms of the purchase or first successful restore. 0 = never. */
    val unlockedAtEpochMs: Long = 0L,

    /** The one-time "taps do nothing now" card has been dismissed. */
    val hasSeenDisplayModeHint: Boolean = false,

    /** Caps rapid movement and blinking in every mood. */
    val reduceFlashing: Boolean = false,

    val muteAllSound: Boolean = false,

    /** Moods whose photosensitivity warning has already been shown. */
    val flashingWarningsSeen: Set<String> = emptySet(),

    // User actions
    val feedbacksGiven: Int = 0,
    val bugsReported: Int = 0,

    /** Epoch-ms — first observed by the review coordinator. 0 = uncaptured. */
    val reviewInstallAt: Long = 0L,

    /** Epoch-ms — last review prompt the coordinator forwarded to the platform. 0 = never. */
    val lastReviewPromptAt: Long = 0L,
) {
    /**
     * Get the visit count for a screen by its tracking key.
     */
    fun getVisitCount(trackingKey: String): Int = screenVisits[trackingKey] ?: 0
    
    /**
     * Increment the visit count for a screen.
     */
    fun incrementVisit(trackingKey: String): AppData = copy(
        screenVisits = screenVisits + (trackingKey to (getVisitCount(trackingKey) + 1))
    )
}

interface AppCache : Cache<AppData>

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, boundType = AppCache::class)
@Inject
class AppCacheImpl(
    cacheFactory: CacheFactory
) : AppCache, Cache<AppData> by cacheFactory.persistent(
    name = "app_data",
    serializer = versionedJsonSerializer(
        defaultValue = { AppData() },
    )
)

