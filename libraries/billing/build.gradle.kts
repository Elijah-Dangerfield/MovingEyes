plugins {
    id("movingeyes.kotlin.multiplatform")
}

moduleConfig {
    di()
    serialization()
}

android {
    namespace = "com.dangerfield.movingeyes.libraries.billing"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.libraries.core)
            // RealPurchasesEnabled is a ConfiguredValue and lives next to the
            // billing api, so any consumer sees the flag with the same import.
            implementation(projects.libraries.config)
            api(libs.kotlinx.coroutines.core)
        }
    }
}
