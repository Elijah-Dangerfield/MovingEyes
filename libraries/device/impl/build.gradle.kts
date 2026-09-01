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
        commonTest.dependencies {
            implementation(projects.libraries.device)
        }
    }
}
