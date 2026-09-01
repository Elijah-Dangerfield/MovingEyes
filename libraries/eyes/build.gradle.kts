plugins {
    id("movingeyes.kotlin.multiplatform")
}

android {
    namespace = "com.dangerfield.movingeyes.libraries.eyes"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.libraries.core)
        }
    }
}
