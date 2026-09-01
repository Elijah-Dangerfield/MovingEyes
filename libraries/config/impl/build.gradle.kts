plugins {
    id("movingeyes.kotlin.multiplatform")
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

moduleConfig {
    di()
    serialization()
}

android {
    namespace = "com.dangerfield.movingeyes.libraries.config.impl"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.libraries.config)
            implementation(projects.libraries.core)
            implementation(projects.libraries.flowroutines)
            implementation(projects.libraries.storage)
            // SessionTracker — drives session-aware refresh on cold boot
            // and ≥15-min-background rollover, replacing the previous
            // fixed-interval polling loop.
            implementation(projects.libraries.movingeyes)
            // `api`: ConfigDiModule provides a Json into the app graph, so the app
            // module has to be able to resolve the type when it merges the graph.
            api(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(compose.components.resources)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }

        commonTest.dependencies {
            implementation(projects.libraries.flowroutines.testing)
            implementation(projects.libraries.config)
            implementation(projects.libraries.core)
            implementation(projects.libraries.storage)
            implementation(projects.libraries.movingeyes)
        }
    }
}

compose.resources {
    publicResClass = false
}