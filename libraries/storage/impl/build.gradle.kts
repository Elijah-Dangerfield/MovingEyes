plugins {
    id("movingeyes.kotlin.multiplatform")
}

android {
    namespace = "com.dangerfield.movingeyes.libraries.storage.impl"
}

moduleConfig.storage()

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.libraries.storage)

            implementation(projects.libraries.core)
            implementation(projects.libraries.flowroutines)
            implementation(projects.libraries.movingeyes)
            implementation(projects.libraries.scene.storage)
            implementation(libs.kotlinx.serialization.json)
        }
    }
}

tasks.matching { it.name.contains("kspCommonMainKotlinMetadata", ignoreCase = true) }
    .configureEach { enabled = false }