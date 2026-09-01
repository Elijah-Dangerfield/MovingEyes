plugins {
    id("movingeyes.kotlin.multiplatform")
    alias(libs.plugins.sentryKmp)
}

moduleConfig {
    optIn("kotlin.time.ExperimentalTime")
    optIn("kotlin.uuid.ExperimentalUuidApi")
    optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
}

android {
    namespace = "com.dangerfield.movingeyes.libraries.movingeyes.impl"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.libraries.movingeyes)

            implementation(projects.libraries.core)
            implementation(libs.kermit)
            implementation(projects.libraries.flowroutines)
            implementation(projects.libraries.movingeyes.storage)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)
        }

        commonTest.dependencies {
            implementation(projects.libraries.movingeyes)
            // :libraries:core for AutoInit (AppEventDispatcher's supertype —
            // the test compiler has to load it to type-check the class).
            implementation(projects.libraries.core)
            implementation(projects.libraries.flowroutines.testing)
        }
    }
}
