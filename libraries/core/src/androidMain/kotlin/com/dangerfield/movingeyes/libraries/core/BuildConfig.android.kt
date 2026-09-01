package com.dangerfield.movingeyes.libraries.core

import com.dangerfield.movingeyes.buildinfo.MovingEyesBuildConfig
import com.dangerfield.movingeyes.libraries.core.BuildConfig as AndroidBuildConfig

actual object BuildInfo {
    actual val isDebug: Boolean
        get() = AndroidBuildConfig.DEBUG

    actual val platform: Platform
        get() = Platform.Android

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