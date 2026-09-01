plugins {
    id("movingeyes.kotlin.multiplatform")
}

moduleConfig {
    di()
}

android {
    namespace = "com.dangerfield.movingeyes.libraries.device.impl"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.libraries.device)
            implementation(projects.libraries.core)
        }
        androidMain.dependencies {
            // ActivityProvider — the host app's contract for handing over the
            // foreground Activity, which window flags need.
            implementation(projects.libraries.movingeyes)
            implementation(libs.androidx.core.ktx)
        }
        commonTest.dependencies {
            implementation(projects.libraries.device)
        }
    }
}
