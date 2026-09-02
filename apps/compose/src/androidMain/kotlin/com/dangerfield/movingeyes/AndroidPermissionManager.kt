package com.dangerfield.movingeyes

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.dangerfield.movingeyes.libraries.movingeyes.ActivityProvider
import com.dangerfield.movingeyes.libraries.movingeyes.Permission
import com.dangerfield.movingeyes.libraries.movingeyes.PermissionManager
import com.dangerfield.movingeyes.libraries.movingeyes.PermissionResult
import com.dangerfield.movingeyes.libraries.movingeyes.PermissionStatus
import kotlinx.coroutines.suspendCancellableCoroutine
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn
import kotlin.coroutines.resume

/**
 * Registers against the foreground activity's `ActivityResultRegistry` rather
 * than requiring `MainActivity` to declare a launcher, so adding a permission
 * doesn't mean touching the host activity.
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class AndroidPermissionManager(
    private val context: Context,
    private val activityProvider: ActivityProvider,
) : PermissionManager {

    /**
     * Always asks when not already granted, rather than pre-classifying.
     * `shouldShowRequestPermissionRationale` is false both for "never asked"
     * and "denied permanently", so deciding up front means never prompting at
     * all; the system returns immediately when it has already been refused.
     */
    override suspend fun ensurePermission(permission: Permission): PermissionResult =
        if (checkPermissionStatus(permission) == PermissionStatus.GRANTED) {
            PermissionResult.Granted
        } else {
            requestPermission(permission)
        }

    override suspend fun requestPermission(permission: Permission): PermissionResult {
        val activity = activityProvider.currentActivity() as? ComponentActivity
            ?: return PermissionResult.Denied(canRequestAgain = true)

        return suspendCancellableCoroutine { continuation ->
            val key = "permission-${permission.manifestName}-${continuation.hashCode()}"
            val launcher = activity.activityResultRegistry.register(
                key,
                ActivityResultContracts.RequestPermission(),
            ) { granted ->
                continuation.resume(
                    if (granted) {
                        PermissionResult.Granted
                    } else {
                        PermissionResult.Denied(
                            canRequestAgain = ActivityCompat
                                .shouldShowRequestPermissionRationale(activity, permission.manifestName),
                        )
                    },
                )
            }
            continuation.invokeOnCancellation { launcher.unregister() }
            launcher.launch(permission.manifestName)
        }
    }

    /**
     * Only GRANTED is reliable. Android gives no way to tell "never asked" from
     * "denied permanently", so everything else reports NOT_DETERMINED and the
     * answer comes from actually asking.
     */
    override fun checkPermissionStatus(permission: Permission): PermissionStatus =
        if (ContextCompat.checkSelfPermission(context, permission.manifestName) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            PermissionStatus.GRANTED
        } else {
            PermissionStatus.NOT_DETERMINED
        }

    override fun openAppSettings() {
        context.startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

private val Permission.manifestName: String
    get() = when (this) {
        Permission.Notifications -> Manifest.permission.POST_NOTIFICATIONS
        Permission.Microphone -> Manifest.permission.RECORD_AUDIO
    }
