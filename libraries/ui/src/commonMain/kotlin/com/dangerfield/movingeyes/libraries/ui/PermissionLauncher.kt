package com.dangerfield.movingeyes.libraries.ui

import androidx.compose.runtime.Composable

/**
 * There is no camera counterpart, and adding one back is a store rejection.
 *
 * Apple scans the uploaded binary for API references rather than call sites, so
 * an unused `AVCaptureDevice` launcher still demands `NSCameraUsageDescription`
 * in `Info.plist` and fails delivery with ITMS-90683 about twenty minutes after
 * an upload that looked fine. Declaring the purpose string silences it and is
 * worse: it claims access to a sensor this app never touches and contradicts
 * the App Privacy answers.
 */
@Composable
expect fun rememberMicrophonePermissionLauncher(
    onResult: (granted: Boolean) -> Unit
): PermissionLauncher

interface PermissionLauncher {
    fun launch()
}
