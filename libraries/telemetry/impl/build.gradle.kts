plugins {
    id("movingeyes.kotlin.multiplatform")
}

moduleConfig {
    di()
    optIn("io.opentelemetry.kotlin.ExperimentalApi")
}

android {
    namespace = "com.dangerfield.movingeyes.libraries.telemetry.impl"
}

// The OTel exporters bring in ktor-client-cio, and CIO on Native throws
// "TLS sessions are not supported on Native platform" for any https request.
// We never ask for CIO, but Ktor resolves an engine off the classpath whenever
// HttpClient is built without an explicit one, and CIO was winning that race
// for every request the iOS app made. That is why remote config silently
// stopped updating on iOS and the OTLP export never left the device. Darwin is
// declared per-platform below and should be the only engine resolvable there.
configurations.configureEach {
    exclude(group = "io.ktor", module = "ktor-client-cio")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.libraries.core)
            implementation(projects.libraries.config)
            // AppEventListener — app.launched must wait for the cold-boot
            // dispatch so it carries the settled session_id.
            implementation(projects.libraries.movingeyes)
            // FileManager — the on-device buffer directory for the durable
            // export pipeline.
            implementation(projects.libraries.storage)
            implementation(projects.libraries.flowroutines)
            implementation(libs.okio)
            implementation(libs.otel.kotlin.api)
            implementation(libs.otel.kotlin.sdk.api)
            implementation(libs.otel.kotlin.implementation)
            implementation(libs.otel.kotlin.exporters.core)
            implementation(libs.otel.kotlin.exporters.persistence)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.contentNegotiation)
            implementation(libs.ktor.client.encoding)
        }

        commonTest.dependencies {
            implementation(projects.libraries.core)
            implementation(projects.libraries.movingeyes)
            implementation(projects.libraries.flowroutines)
            implementation(projects.libraries.flowroutines.testing)
            implementation(libs.okio)
            implementation(libs.otel.kotlin.api)
            implementation(libs.otel.kotlin.sdk.api)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.mock)
        }

        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}
