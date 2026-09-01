plugins {
    id("movingeyes.kotlin.multiplatform")
}

moduleConfig {
    serialization()
}

android {
    namespace = "com.dangerfield.movingeyes.libraries.scene"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.libraries.core)
            // `api`, not `implementation`: a Scene is made of EyeStyleIds, Moods
            // and BehaviorConfigs, so anyone holding a Scene needs those types.
            api(projects.libraries.eyes)
            // Declared here as well as via moduleConfig.serialization(), which
            // only reaches the default configuration — enough for @Serializable
            // (a compiler plugin) but not for the Json API in commonMain, which
            // compiles on Android and fails on iOS.
            implementation(libs.kotlinx.serialization.json)
        }
    }
}
