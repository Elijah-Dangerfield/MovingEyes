plugins {
  id("movingeyes.feature")
}

android {
  namespace = "com.dangerfield.movingeyes.features.onboarding"
}

kotlin {
  sourceSets {
    commonMain.dependencies {
      implementation(projects.libraries.navigation)

      implementation(projects.libraries.core)
      implementation(projects.libraries.ui)

      // Compose dependencies (navigation and lifecycle provided by movingeyes.feature plugin)
      implementation(compose.runtime)
      implementation(compose.foundation)
      implementation(compose.material3)
      implementation(compose.ui)
      implementation(compose.components.resources)
      implementation(compose.components.uiToolingPreview)
    }
  }
}
