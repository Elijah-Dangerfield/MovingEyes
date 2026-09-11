package com.dangerfield.movingeyes.libraries.core

expect object BuildInfo {
    val isDebug: Boolean
    val platform: Platform
    val applicationId: String
    val versionName: String
    val versionCode: Int
    val releaseChannel: String
    val buildNumber: Int

    /** Short git SHA the build was produced from — `GITHUB_SHA` in CI,
     *  `git rev-parse` locally, `"unknown"` when neither is available. */
    val commitSha: String

    /** Branch the build was produced from (`GITHUB_REF_NAME` in CI). */
    val commitBranch: String
}

fun BuildInfo.isiOS() = BuildInfo.platform == Platform.iOS
val BuildInfo.buildType: String get() = if (BuildInfo.isDebug) "debug" else "release"

/**
 * Builds where the QA tools are reachable: debug, plus TestFlight and Play
 * internal, which CI stamps `beta`. Production is stamped `store` and is the
 * only channel this excludes.
 *
 * Beta is in here because of one thing that cannot be tested without it. A
 * grant is deliberately never revoked (see `Entitlements`), and the only way
 * back to a locked state is the QA menu's Clear entitlement. Gating that on
 * `isDebug` alone meant a tester who unlocked once, even against the fake
 * store on a debug build, kept the unlock through every later install and
 * could never reach the paywall again. Delete-and-reinstall was the only
 * remedy, which is not a thing a tester thinks to do.
 */
val BuildInfo.isQaBuild: Boolean get() = BuildInfo.isDebug || BuildInfo.releaseChannel == "beta"
val BuildInfo.versionTag: String get() = "${BuildInfo.versionName}-${BuildInfo.releaseChannel}"
fun BuildInfo.versionString(): String = "$versionName ($buildNumber)"


enum class Platform {
    Android,
    iOS
}