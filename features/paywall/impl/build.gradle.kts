plugins {
    id("movingeyes.feature")
}

android {
    namespace = "com.dangerfield.movingeyes.features.paywall.impl"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.features.paywall)
            implementation(projects.libraries.navigation)

            implementation(projects.libraries.core)
            implementation(projects.libraries.flowroutines)
            implementation(projects.libraries.ui)
            implementation(projects.libraries.resources)
            implementation(projects.libraries.billing)
            implementation(projects.libraries.eyes)
            implementation(projects.libraries.render)

            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
        }
    }
}
