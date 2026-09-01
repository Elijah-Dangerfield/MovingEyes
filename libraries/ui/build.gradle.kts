
plugins {
    id("movingeyes.compose.multiplatform")
}

android {
    namespace = "com.dangerfield.movingeyes.libraries.ui"
}

kotlin {
    sourceSets {

        androidMain.dependencies {
            api(compose.preview)
            api(compose.uiTooling)
        }

        commonMain.dependencies {
            implementation(projects.libraries.core)
            // TODO honestly the movingeyes library should expose the component that require movingeyes domain
            implementation(projects.libraries.movingeyes)
            implementation(projects.libraries.resources)

            api(compose.ui)
            api(compose.uiUtil)
            api(compose.runtime)
            api(compose.foundation)
            api(compose.material3)
            api(compose.components.resources)
            api(compose.components.uiToolingPreview)
            api(compose.materialIconsExtended)
            api(compose.material3AdaptiveNavigationSuite)
            api(libs.compose.backhandler)
        }
    }
}