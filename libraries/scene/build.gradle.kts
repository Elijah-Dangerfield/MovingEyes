plugins {
    id("movingeyes.kotlin.multiplatform")
}

android {
    namespace = "com.dangerfield.movingeyes.libraries.scene"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.libraries.core)
            // api: a Scene's public shape is made of EyeStyleIds, Moods and
            // BehaviorConfigs.
            api(projects.libraries.eyes)
            implementation(libs.kotlinx.serialization.json)
        }
    }
}
