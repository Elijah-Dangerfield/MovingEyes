package com.dangerfield.movingeyes.libraries.core

import com.dangerfield.movingeyes.buildinfo.MovingEyesBuildConfig
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform as NativePlatform

@OptIn(ExperimentalNativeApi::class)
actual object BuildInfo {
    actual val isDebug: Boolean
        get() = NativePlatform.isDebugBinary

    actual val platform: Platform = Platform.iOS

    actual val applicationId: String
        get() = MovingEyesBuildConfig.APPLICATION_ID

    actual val versionName: String
        get() = MovingEyesBuildConfig.VERSION_NAME

    actual val versionCode: Int
        get() = MovingEyesBuildConfig.VERSION_CODE

    actual val releaseChannel: String
        get() = MovingEyesBuildConfig.RELEASE_CHANNEL

    actual val buildNumber: Int
        get() = MovingEyesBuildConfig.BUILD_NUMBER

    actual val commitSha: String
        get() = MovingEyesBuildConfig.COMMIT_SHA

    actual val commitBranch: String
        get() = MovingEyesBuildConfig.COMMIT_BRANCH
}