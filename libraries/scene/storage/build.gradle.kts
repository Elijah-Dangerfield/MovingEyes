plugins {
    id("movingeyes.kotlin.multiplatform")
}

android {
    namespace = "com.dangerfield.movingeyes.libraries.scene.storage"
}

moduleConfig.storage()

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.libraries.core)
            implementation(projects.libraries.flowroutines)
            implementation(projects.libraries.storage)
            api(projects.libraries.scene)
        }
    }
}
