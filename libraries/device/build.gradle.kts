plugins {
    id("movingeyes.kotlin.multiplatform")
}

android {
    namespace = "com.dangerfield.movingeyes.libraries.device"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.libraries.core)
            api(libs.kotlinx.coroutines.core)
        }
    }
}
