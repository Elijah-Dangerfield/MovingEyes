plugins {
    id("movingeyes.kotlin.multiplatform")
}

moduleConfig {
    di()
    optIn("kotlin.time.ExperimentalTime")
}

android {
    namespace = "com.dangerfield.movingeyes.libraries.billing.impl"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.libraries.billing)
            implementation(projects.libraries.core)
            implementation(projects.libraries.config)
            implementation(projects.libraries.flowroutines)
            // AppCache holds the cached entitlement; ActivityProvider (android)
            // supplies the foreground Activity Play needs for its purchase flow.
            implementation(projects.libraries.movingeyes)
            implementation(libs.kotlinx.datetime)
        }
        androidMain.dependencies {
            implementation(libs.google.play.billing.ktx)
        }
        commonTest.dependencies {
            implementation(projects.libraries.flowroutines.testing)
            implementation(projects.libraries.billing)
            implementation(projects.libraries.core)
            implementation(projects.libraries.config)
            implementation(projects.libraries.movingeyes)
        }
    }
}
