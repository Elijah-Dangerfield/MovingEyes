package com.dangerfield.movingeyes

import com.dangerfield.movingeyes.libraries.identity.auth.AppleSignInCoordinator
import com.dangerfield.movingeyes.libraries.identity.auth.SecureSessionStorage
import com.dangerfield.movingeyes.libraries.movingeyes.PermissionManager
import com.dangerfield.movingeyes.libraries.review.ReviewLauncher
import com.dangerfield.movingeyes.libraries.ui.nativeviews.NativeViewFactory
import me.tatarka.inject.annotations.Provides
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.MergeComponent
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@MergeComponent(AppScope::class)
@SingleIn(AppScope::class)
abstract class IosAppComponent(
    private val permissionManager: PermissionManager,
    private val reviewLauncher: ReviewLauncher,
    // The Swift `IOSAppleSignInCoordinator` (ASAuthorizationController flow),
    // passed in from `iOSApp.swift`. Android binds its own no-op via anvil.
    private val appleSignInCoordinator: AppleSignInCoordinator,
    // The Swift `IOSSecureSessionStorage` (Keychain-backed Supabase session
    // store), passed in from `iOSApp.swift`. Android binds
    // EncryptedSessionStorage via anvil.
    private val secureSessionStorage: SecureSessionStorage,
    val nativeViewFactory: NativeViewFactory
) : AppComponent {

    @Provides
    fun providePermissionManager(): PermissionManager = permissionManager

    @Provides
    fun provideReviewLauncher(): ReviewLauncher = reviewLauncher

    @Provides
    fun provideAppleSignInCoordinator(): AppleSignInCoordinator = appleSignInCoordinator

    @Provides
    fun provideSecureSessionStorage(): SecureSessionStorage = secureSessionStorage
}


@MergeComponent.CreateComponent
expect fun create(
    permissionManager: PermissionManager,
    reviewLauncher: ReviewLauncher,
    appleSignInCoordinator: AppleSignInCoordinator,
    secureSessionStorage: SecureSessionStorage,
    nativeViewFactory: NativeViewFactory
): IosAppComponent
