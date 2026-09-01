plugins {
    id("movingeyes.feature")
}

android {
    namespace = "com.dangerfield.movingeyes.features.settings"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.libraries.navigation)
            implementation(compose.runtime)
        }
    }
}
