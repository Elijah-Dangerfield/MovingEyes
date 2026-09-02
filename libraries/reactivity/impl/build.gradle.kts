plugins {
    id("movingeyes.kotlin.multiplatform")
}

moduleConfig {
    di()
}

android {
    namespace = "com.dangerfield.movingeyes.libraries.reactivity.impl"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.libraries.reactivity)
            implementation(projects.libraries.core)
            implementation(projects.libraries.flowroutines)
            implementation(projects.libraries.movingeyes)
        }
        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
        }
        commonTest.dependencies {
            implementation(projects.libraries.flowroutines.testing)
        }
    }
}
