plugins {
    id("movingeyes.application")
    id("co.touchlab.skie") version "0.10.12"

}

android {
    namespace = "com.dangerfield.movingeyes"
}

kotlin {

    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.splashscreen)
            implementation(libs.androidx.work.runtime)
            implementation(compose.uiTooling)
        }

        commonMain.dependencies {
            // Project dependencies
            api(projects.libraries.core)
            implementation(projects.libraries.ui)
            implementation(projects.libraries.movingeyes)
            implementation(projects.libraries.movingeyes.impl)
            implementation(projects.libraries.flowroutines)
            implementation(projects.libraries.navigation)
            implementation(projects.libraries.navigation.impl)
            implementation(projects.libraries.resources)
            implementation(projects.libraries.review)
            implementation(projects.libraries.review.impl)

            implementation(projects.libraries.storage)
            implementation(projects.libraries.storage.impl)
            implementation(projects.libraries.scene.storage)
            implementation(projects.libraries.device)
            implementation(projects.libraries.device.impl)
            implementation(projects.libraries.reactivity)
            implementation(projects.libraries.reactivity.impl)
            implementation(projects.libraries.eyes)
            implementation(projects.libraries.render)
            implementation(projects.libraries.billing)
            implementation(projects.libraries.billing.impl)
            implementation(projects.libraries.config)
            implementation(projects.libraries.config.impl)
            implementation(projects.libraries.telemetry.impl)

            implementation(projects.features.editor)
            implementation(projects.features.editor.impl)
            implementation(projects.features.paywall.impl)
            implementation(projects.features.settings.impl)

            implementation(libs.atomicfu)
            
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.materialIconsExtended)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
        }
    }
}