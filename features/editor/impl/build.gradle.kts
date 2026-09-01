plugins {
    id("movingeyes.feature")
}

android {
    namespace = "com.dangerfield.movingeyes.features.editor.impl"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.features.editor)
            implementation(projects.libraries.navigation)

            implementation(projects.libraries.core)
            implementation(projects.libraries.flowroutines)
            implementation(projects.libraries.ui)
            implementation(projects.libraries.resources)
            implementation(projects.libraries.device)
            implementation(projects.libraries.eyes)
            implementation(projects.libraries.render)
            implementation(projects.libraries.movingeyes)

            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
        }

        commonTest.dependencies {
            implementation(projects.libraries.flowroutines.testing)
        }
    }
}
