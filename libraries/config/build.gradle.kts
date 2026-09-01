plugins {
    id("movingeyes.kotlin.multiplatform")
}

android {
    namespace = "com.dangerfield.movingeyes.libraries.config"
}


kotlin {
    sourceSets {
        commonMain.dependencies {

            implementation(projects.libraries.core)
            implementation(projects.libraries.flowroutines)
            implementation(libs.kotlin.inject.runtime.kmp)
            implementation(libs.kotlinx.serialization.json)
        }
    }
}