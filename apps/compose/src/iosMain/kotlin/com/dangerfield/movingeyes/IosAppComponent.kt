package com.dangerfield.movingeyes

import com.dangerfield.movingeyes.libraries.billing.StoreKitCoordinator
import com.dangerfield.movingeyes.libraries.device.DisplayHost
import com.dangerfield.movingeyes.libraries.movingeyes.PermissionManager
import com.dangerfield.movingeyes.libraries.review.ReviewLauncher
import me.tatarka.inject.annotations.Provides
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.MergeComponent
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@MergeComponent(AppScope::class)
@SingleIn(AppScope::class)
abstract class IosAppComponent(
    private val permissionManager: PermissionManager,
    private val reviewLauncher: ReviewLauncher,
    // The Swift `IOSStoreKitCoordinator`, passed in from `iOSApp.swift`.
    // StoreKit 2's purchase APIs are Swift-only async, so the implementation
    // has to live over there. Android binds a no-op via anvil.
    private val storeKitCoordinator: StoreKitCoordinator,
    // The Swift `IOSDisplayHost`. Hiding the status bar and home indicator and
    // freezing rotation are all view-controller overrides on iOS with no
    // imperative UIKit equivalent, so they can't be reached from Kotlin/Native.
    // Android binds a no-op via anvil. See `DisplayHost`.
    private val displayHost: DisplayHost,
) : AppComponent {

    @Provides
    fun providePermissionManager(): PermissionManager = permissionManager

    @Provides
    fun provideReviewLauncher(): ReviewLauncher = reviewLauncher

    @Provides
    fun provideStoreKitCoordinator(): StoreKitCoordinator = storeKitCoordinator

    @Provides
    fun provideDisplayHost(): DisplayHost = displayHost
}


@MergeComponent.CreateComponent
expect fun create(
    permissionManager: PermissionManager,
    reviewLauncher: ReviewLauncher,
    storeKitCoordinator: StoreKitCoordinator,
    displayHost: DisplayHost,
): IosAppComponent
