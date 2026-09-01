enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "MovingEyes"

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        google()
        mavenCentral()
    }
}

// Apps
include(":apps")
include(":apps:compose")
// Note: iOS app is not a Gradle module - it's an Xcode project in apps/ios/

// Features
include(":features:editor")
include(":features:editor:impl")

// Libraries
include(":libraries:billing")
include(":libraries:billing:impl")
include(":libraries:config")
include(":libraries:config:impl")
include(":libraries:core")
// Pure Kotlin, no Compose: the behaviour engine is driven by an injected clock
// so a synthetic one can assert timing without rendering anything.
include(":libraries:eyes")
include(":libraries:flowroutines")
include(":libraries:flowroutines:testing")
include(":libraries:navigation")
include(":libraries:navigation:impl")
include(":libraries:render")
include(":libraries:resources")
include(":libraries:review")
include(":libraries:review:impl")
include(":libraries:storage")
include(":libraries:storage:impl")
include(":libraries:movingeyes")
include(":libraries:movingeyes:impl")
include(":libraries:movingeyes:storage")
// No api sibling on purpose: the public surface is the `logEvent` extension in
// :libraries:core; this impl only hosts the OTel dependency + the Grafana
// log-tree wiring.
include(":libraries:telemetry:impl")
include(":libraries:ui")

// Custom detekt rules — a standalone JVM jar detekt loads via `detektPlugins`.
// Dev/CI tooling only, never shipped.
include(":detekt-rules")
