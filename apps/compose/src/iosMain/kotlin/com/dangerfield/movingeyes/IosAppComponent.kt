package com.dangerfield.movingeyes

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
) : AppComponent {

    @Provides
    fun providePermissionManager(): PermissionManager = permissionManager

    @Provides
    fun provideReviewLauncher(): ReviewLauncher = reviewLauncher
}


@MergeComponent.CreateComponent
expect fun create(
    permissionManager: PermissionManager,
    reviewLauncher: ReviewLauncher,
): IosAppComponent
