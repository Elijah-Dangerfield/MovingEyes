import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

group = "com.dangerfield.movingeyes.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

repositories {
    gradlePluginPortal()
    google()
    mavenCentral()
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
    api(libs.ksp.gradlePlugin)
    api(libs.androidx.room.gradlePlugin)
    api(libs.kotlinx.serialization.gradlePlugin)
    api(libs.buildconfig.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("kotlinMultiplatform") {
            id = "movingeyes.kotlin.multiplatform"
            implementationClass = "com.dangerfield.movingeyes.plugin.KotlinMultiplatformConventionPlugin"
        }
        register("composeMultiplatform") {
            id = "movingeyes.compose.multiplatform"
            implementationClass = "com.dangerfield.movingeyes.plugin.ComposeMultiplatformConventionPlugin"
        }
        register("feature") {
            id = "movingeyes.feature"
            implementationClass = "com.dangerfield.movingeyes.plugin.FeatureConventionPlugin"
        }
        register("application") {
            id = "movingeyes.application"
            implementationClass = "com.dangerfield.movingeyes.plugin.ApplicationConventionPlugin"
        }
    }
}