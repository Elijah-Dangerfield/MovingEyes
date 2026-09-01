plugins {
    id("movingeyes.compose.multiplatform")
}

android {
    namespace = "com.dangerfield.movingeyes.libraries.render"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.libraries.core)
            implementation(projects.libraries.eyes)
            api(projects.libraries.scene)

            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(compose.components.uiToolingPreview)
        }
    }
}
