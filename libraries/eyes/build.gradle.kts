plugins {
    id("movingeyes.kotlin.multiplatform")
}

moduleConfig {
    serialization()
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
